//
// Created by Administrator on 2019/12/5.
//

#include "include/ffmpeg/ffmpeg_handle.h"
#include <jni.h>
#include <string>
#include <android/log.h>
#include <sys/time.h>

extern "C" {
#include <libavcodec/avcodec.h>
#include <libavformat/avformat.h>
#include <libavutil/time.h>
#include <libavutil/opt.h>
//像素处理
#include <android/native_window_jni.h>
#include <libswscale/swscale.h>
#include <unistd.h>
#include <libavutil/imgutils.h>
}

#define logw(content)   __android_log_write(ANDROID_LOG_WARN,"zqwx",content)
#define loge(content)   __android_log_write(ANDROID_LOG_ERROR,"zqwx",content)
#define logd(content)   __android_log_write(ANDROID_LOG_DEBUG,"zqwx",content)
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, "zqwx" ,__VA_ARGS__) // 定义LOGD类型

using namespace std;

double r2d(AVRational r) {
    return r.num == 0 || r.den == 0 ? 0. : (double) r.num / (double) r.den;
}

int ffmpeg_publish_using_packet(const char *rtmpURL, const char *filePath) {
    //所有代码执行之前要调用av_register_all和avformat_network_init
    //初始化所有的封装和解封装 flv mp4 mp3 mov。不包含编码和解码
    av_register_all();

    //初始化网络库
    avformat_network_init();


    /**
     //////////////////////////////////////////////////////////////////
     //                   输入流处理部分
     /////////////////////////////////////////////////////////////////
     打开文件， 解封装 avformat_open_input
     AVFormatContext **ps 输入封装的上下文。包含所有的格式内容和所有的IO。如果是文件就是文件IO，网络就对应网络IO
     const char *url 路径
     AVInputFormat *fmt 封装器
     AVDictionary **options 参数设置

     **/
    AVFormatContext *ictx = NULL;

    //打开文件，解封文件头
    int ret = avformat_open_input(&ictx, filePath, 0, NULL);
    if (ret < 0) {
        logw("解封文件头失败");
        return ret;
        //goto end;
    }
    logw("avformat_open_input success!");
    //获取音视频的信息 .h264 flv 没有头信息
    ret = avformat_find_stream_info(ictx, 0);
    if (ret != 0) {
        loge("获取音视频信息失败");
        return ret;
        //goto end;
    }
    av_dump_format(ictx, 0, filePath, 0);

    /**
     * //////////////////////////////////////////////////////////////////
       //                   输出流处理部分
       /////////////////////////////////////////////////////////////////

     */
    AVFormatContext *octx = NULL;
    //如果是输入文件 flv可以不传，可以从文件中判断。如果是流则必须传
    //创建输出上下文
    ret = avformat_alloc_output_context2(&octx, NULL, "flv", rtmpURL);
    if (ret < 0) {
        loge("创建输出上下文失败！");
        return ret;
        //goto end;
    }
    logw("avformat_alloc_output_context2 success!");


    /**
     * 配置输出流
     * AVIOcontext *pb  IO上下文
     * AVStream **stream    指针数组，存放多个输出流 视频音频字幕流
     * int nb_streams;
     * duration, bit_rate
     *
     * AVStream
     * AVRational time_base
     * AVCodecParameters *codecpar  音视频参数
     * AVCodecContext *codec
     * 遍历输入的AVStream
     */
//    ictx -> probesize /= 3;
//    ictx -> max_analyze_duration /= 3;


    int i;
    for (i = 0; i < ictx->nb_streams; ++i) {
        // AV_CODEC_FLAG_GLOBAL_HEADER  -- 将全局头文件放在引渡文件中，而不是每个关键帧中。
        //AV_CODEC_FLAG_LOW_DELAY  --较低延迟
        ictx->streams[i]->codec->flags |= AV_CODEC_FLAG_GLOBAL_HEADER | AV_CODEC_FLAG_LOW_DELAY;;
//        ictx -> streams[i] -> codec -> max_b_frames = 0;
        //实时推流，零延迟
        av_opt_set(ictx->streams[i]->codec->priv_data, "tune", "zerolatency", 0);
        //创建一个新的流到octx中
        AVStream *out = avformat_new_stream(octx, avcodec_find_decoder(
                ictx->streams[i]->codecpar->codec_id));
        if (!out) {
            loge("创建新的流octx失败！");
            return ret;
            //goto end;
        }

        //复制配置信息 用于mp4过时的方法
        ret = avcodec_parameters_copy(out->codecpar, ictx->streams[i]->codecpar);
        if (ret < 0) {
            loge("复制配置信息失败！");
            return ret;
            //goto end;
        }
        out->codecpar->codec_tag = 0;

    }

    int videoIndex;
    //输入流数据的数量循环
    for (i = 0; i < ictx->nb_streams; ++i) {
        if (ictx->streams[i]->codecpar->codec_type == AVMEDIA_TYPE_VIDEO) {
            videoIndex = i;
            break;
        }
    }

    av_dump_format(octx, 0, rtmpURL, 1);

    /**
     *
     * //////////////////////////////////////////////////////////////////
       //                   准备推流
       /////////////////////////////////////////////////////////////////

     */
    //打开IO
    ret = avio_open(&octx->pb, rtmpURL, AVIO_FLAG_WRITE);
    if (ret < 0) {
        loge("打开推流IO失败！");
        return ret;
        //goto end;
    }

    //写入头部信息
    ret = avformat_write_header(octx, 0);
    if (ret < 0) {
        loge("写入头部信息失败！");
        return ret;
        //goto end;
    }
    logw("avformat_write_header Success!");

    /**
     * 推流每一条数据
     * int64_t pts [pts * (num / den) 第几秒显示]
     * int64_t dts 解码时间 [P帧（相对于上一帧的变化） I帧（关键帧，完整的数据） B帧（上一帧和下一帧的变化） 有了B帧压缩率更高]
     * uint8_t *data
     * int size
     * int stream_index
     * int flag
     */
    AVPacket avPacket;
    //获取当前的时间戳 微妙
    long long startTime = av_gettime();
    long long frame_index = 0;

    while (true) {
        //输入、输出视频流
        AVStream *in_stream, *out_stream;
        ret = av_read_frame(ictx, &avPacket);
        if (ret < 0) {
            LOGD("read frame failure ret : %i", ret);
            break;
        }
        LOGD("AVPacket.pts : %lld", avPacket.pts);

        /**
         * PTS （Presentation Time Stamp）显示播放时间
         * DTS （Decoding Time Stamp）解码时间
         */
        //没有显示时间（比如未解码的H.264）
        if (avPacket.pts == AV_NOPTS_VALUE) {
            //AVRational time_base:时基。通过该值可以把PTS、DTS转化为真正的时间。
            AVRational time_base1 = ictx->streams[videoIndex]->time_base;

            /**
             * 计算两帧之间的时间
             * r_frame_rate 基流帧速率
             * av_q2d 转化为double型
             */
            int64_t calc_duration =
                    (double) AV_TIME_BASE / av_q2d(ictx->streams[videoIndex]->r_frame_rate);

            //配置参数
            avPacket.pts = (double) (frame_index * calc_duration) /
                           (double) (av_q2d(time_base1) * AV_TIME_BASE);
            avPacket.dts = avPacket.pts;
            avPacket.duration =
                    (double) calc_duration / (double) (av_q2d(time_base1) * AV_TIME_BASE);
        }

        //延时
        if (avPacket.stream_index == videoIndex) {
            AVRational time_base = ictx->streams[videoIndex]->time_base;
            AVRational time_base_q = {1, AV_TIME_BASE};
            //计算视频播放时间
            int64_t pts_time = av_rescale_q(avPacket.dts, time_base, time_base_q);
            //计算实际视频的播放时间
            int64_t now_time = av_gettime() - startTime;
            AVRational avr = ictx->streams[videoIndex]->time_base;
            LOGD("avr.num : %i, avr.den : %i, , avPacket.dts : %lld, , avPacket.pts : %lld, , pts_time : %lld, ",
                 avr.num, avr.den, avPacket.dts, avPacket.pts, pts_time);
            if (pts_time > now_time) {
                //睡眠一段时间（目的是让当前视频记录的播放时间与实际时间同步）
                av_usleep((unsigned int) (pts_time - now_time));
            }
        }

        in_stream = ictx->streams[avPacket.stream_index];
        out_stream = octx->streams[avPacket.stream_index];

        //计算延时后，重新指定时间戳

        //计算转换时间戳 pts dts
        //获取时间基数
        AVRational itime = in_stream->time_base;
        AVRational otime = out_stream->time_base;
        avPacket.pts = av_rescale_q_rnd(avPacket.pts, itime, otime,
                                        (AVRounding) (AV_ROUND_NEAR_INF | AV_ROUND_PASS_MINMAX));
        avPacket.dts = av_rescale_q_rnd(avPacket.dts, itime, otime,
                                        (AVRounding) (AV_ROUND_NEAR_INF | AV_ROUND_PASS_MINMAX));
        //到这一帧时间经历了多长时间
        avPacket.duration = (int) av_rescale_q(avPacket.duration, itime, otime);
        avPacket.pos = -1;

        if (avPacket.stream_index == videoIndex) {
            LOGD("Send %lld video frames to output URL", frame_index);
            frame_index++;
        }

        /*//视频帧推送速度
        if(ictx -> streams[avPacket.stream_index] -> codecpar -> codec_type == AVMEDIA_TYPE_VIDEO) {
            AVRational tb = ictx -> streams[avPacket.stream_index] -> time_base;
            //已经过去的时间
            long long now = av_gettime() - startTime;
            long long dts = 0;
            dts = avPacket.dts * (1000 * 1000 * r2d(tb));
            if(dts > now) {
                av_usleep(dts - now);
            } else {
                logw("not sleep!");
            }
        }*/

        //推送会自动释放空间 不需要调用av_packet_unref
        ret = av_interleaved_write_frame(octx, &avPacket);
        if (ret < 0) {
            LOGD("write frame failure ret : %i", ret);
            break;
        }

        //视频帧推送速度
        //if (avPacket.stream_index == 0)
        //  av_usleep(30 * 1000);
        //释放空间。内部指向的视频空间和音频空间
        av_packet_unref(&avPacket);
    }

    if (octx != NULL)
        avio_close(octx->pb);
    //释放输出封装上下文
    if (octx != NULL)
        avformat_free_context(octx);
    //关闭输入上下文
    if (ictx != NULL)
        avformat_close_input(&ictx);

    octx = NULL;
    ictx = NULL;

    return 0;
}

extern "C"
JNIEXPORT void JNICALL
Java_com_michael_libplayer_ffmpeg_FFMpegHandle_pushFFMpegFile(JNIEnv *env, jobject instance,
                                                              jstring rtmpURL_, jstring filePath_) {
    const char *rtmpURL = env->GetStringUTFChars(rtmpURL_, 0);
    const char *path = env->GetStringUTFChars(filePath_, 0);
    logw(path);
    // TODO
    ffmpeg_publish_using_packet(rtmpURL, path);
    env->ReleaseStringUTFChars(filePath_, path);
    env->ReleaseStringUTFChars(rtmpURL_, rtmpURL);
}

static long getCurrentTime() {
    struct timeval tv;
    gettimeofday(&tv, NULL);
    return tv.tv_sec * 1000 + tv.tv_usec / 1000;
}

extern "C"
JNIEXPORT void JNICALL
Java_com_michael_libplayer_ffmpeg_FFMpegHandle_renderFFmpeg(JNIEnv *env, jobject instance,
                                                            jstring path_, jobject surface,
                                                            jint version) {
    const char *path = env->GetStringUTFChars(path_, 0);

    // TODO
    av_register_all();//注册各大组件
    //打开地址并获取里面的内容
    AVFormatContext *avFormatContext = avformat_alloc_context();//获取内容上下文
    avformat_open_input(&avFormatContext, path, NULL, NULL);//解封装
    avformat_find_stream_info(avFormatContext, NULL);
    //找出视频流
    int video_index = -1;
    /*for (int i = 0; i < avFormatContext->nb_streams; ++i) {
        if(avFormatContext->streams[i]->codecpar->codec_type == AVMEDIA_TYPE_VIDEO) {
            video_index = i;
        }
    }*/
    AVCodec *avCodec;
    AVCodecContext *avCodecContext;
    // 找到根据传入参数，找到最适合的数据流，和该数据流的编解码器，这里传入 AVMEDIA_TYPE_VIDEO 表示需要找到视频流
    video_index = av_find_best_stream(avFormatContext, AVMEDIA_TYPE_VIDEO, -1, -1, &avCodec, 0);
    if (video_index < 0) {
        return;
    }
//    AVCodecContext *avCodecContext = avFormatContext->streams[video_index]->codec;//获取解码器上下文
//    AVCodec *avCodec = avcodec_find_decoder(avCodecContext->codec_id);//获取解码器
    avCodecContext = avcodec_alloc_context3(avCodec);
    if (!avCodecContext) {
        return;
    }
    //使用视频流的信息来编解码上下文的参数
    avcodec_parameters_to_context(avCodecContext, avFormatContext->streams[video_index]->codecpar);
    //打开解码器
    if (avcodec_open2(avCodecContext, avCodec, NULL) < 0) {
        logw("打开解码器失败");
        return;
    }
    AVPacket *packet = (AVPacket *) av_malloc(
            sizeof(AVPacket));//申请AVPacket, 用于保存解码之前的数据和一些附加信息，pts、dts、timestamp，所在媒体流索引
    av_init_packet(packet);
    AVFrame *frame = av_frame_alloc();//申请AVFrame, 用于存储原始数据，指向解码后的原始帧
    AVFrame *rgb_frame = av_frame_alloc();//存放转换成rgb后的帧
    //缓存区 内存大小就是 格式为RGBA的视频帧的大小
    int numBytes = av_image_get_buffer_size(AV_PIX_FMT_RGBA, avCodecContext->width, avCodecContext->height, 1);
    uint8_t *out_buffer = (uint8_t *) av_malloc(numBytes * sizeof(uint8_t));
    //与缓存区相关联，设置rgb_frame缓存区
//    avpicture_fill((AVPicture *)rgb_frame, out_buffer, AV_PIX_FMT_RGBA, avCodecContext->width, avCodecContext->height);
    av_image_fill_arrays(rgb_frame->data, rgb_frame->linesize, out_buffer, AV_PIX_FMT_RGBA,
                         avCodecContext->width, avCodecContext->height, 1);

    //由于解码出来的帧格式不是RGBA的，在渲染之前需要进行格式转换
    SwsContext *swsContext = sws_getContext(avCodecContext->width, avCodecContext->height,
                                            avCodecContext->pix_fmt,
                                            avCodecContext->width, avCodecContext->height,
                                            AV_PIX_FMT_RGBA, SWS_BICUBIC, NULL, NULL, NULL);
    //取到NativeWindow
    ANativeWindow *nativeWindow = ANativeWindow_fromSurface(env, surface);
    if (nativeWindow == 0) {
        loge("获取NativeWindow失败");
        return;
    }
    //视频缓冲区
    ANativeWindow_Buffer native_outBuffer;
    int ret;
    //开始解码
    while (av_read_frame(avFormatContext, packet) >= 0) {
        long start_time = getCurrentTime();
        LOGD("解码信息 ： %d, v_index : %d", packet->stream_index, video_index);
        if (packet->stream_index == video_index) {
            logd("开始解码");
//            avcodec_decode_video2(avCodecContext, frame, &frame_count, packet);
            ret = avcodec_send_packet(avCodecContext, packet);
            if(ret < 0) {
                break;
            }
            while(ret >= 0) {
                ret = avcodec_receive_frame(avCodecContext, frame);
                if(ret == AVERROR(EAGAIN) || ret == AVERROR_EOF) {
                    break;
                } else if(ret < 0) {
                    logd("Error while receiving a frame from the decoder");
                }

                logd("转换并绘制");
                //说明有内容，绘制之前配置的NativeWindow
                ANativeWindow_setBuffersGeometry(nativeWindow, avCodecContext->width,
                                                 avCodecContext->height, WINDOW_FORMAT_RGBA_8888);
                //上锁
                ANativeWindow_lock(nativeWindow, &native_outBuffer, NULL);
                //转换为rgb格式
                sws_scale(swsContext, (const uint8_t *const *) frame->data, frame->linesize, 0,
                          frame->height, rgb_frame->data, rgb_frame->linesize);
                //rgb是有画面数据
                uint8_t *dst = (uint8_t *) (native_outBuffer.bits);
                //拿到一行有多少个字节rgba
                int destStride = native_outBuffer.stride * 4;
                //像素数据的首地址
                uint8_t *src = rgb_frame->data[0];
                //实际内存一行数量
                int srcStride = rgb_frame->linesize[0];
                for (int i = 0; i < avCodecContext->height; ++i) {
                    //将rgb_frame中每一行的数据复制给NativeWindow
                    memcpy(dst + i * destStride, src + i * srcStride, srcStride);
                }

                int sleep_time = 40 - (getCurrentTime() - start_time);
                if(sleep_time > 0) {
                    usleep(sleep_time);
                }

                //解锁
                ANativeWindow_unlockAndPost(nativeWindow);
            }
        }

        av_packet_unref(packet);
    }

    logd("播放完毕");

    //释放资源
    if(swsContext) sws_freeContext(swsContext);
    ANativeWindow_release(nativeWindow);
    av_frame_free(&frame);
    av_frame_free(&rgb_frame);
    avcodec_close(avCodecContext);
    avformat_free_context(avFormatContext);
    env->ReleaseStringUTFChars(path_, path);
}