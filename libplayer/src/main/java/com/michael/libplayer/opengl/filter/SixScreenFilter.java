package com.michael.libplayer.opengl.filter;


import com.michael.libplayer.opengl.base.BaseFilter;

/**
 * LazyFilter 绘制原始纹理的Filter,通过矩阵提供旋转缩放等功能
 */
public class SixScreenFilter extends BaseFilter {

    private static final String vertexCode =
            "attribute vec4 aVertexCo;\n" +
                    "attribute vec2 aTextureCo;\n" +
                    "uniform mat4 uVertexMatrix;\n" +
                    "uniform mat4 uTextureMatrix;\n" +
                    "\n" +
                    "varying vec2 vTextureCo;\n" +
                    "\n" +
                    "void main(){\n" +
                    "    gl_Position = uVertexMatrix*aVertexCo;\n" +
                    "    vTextureCo = (uTextureMatrix*vec4(aTextureCo,0,1)).xy;\n" +
                    "}";

    private static final String fragmentCode =
            "precision highp float;\n" +
                    "varying highp vec2 vTextureCo;\n" +
                    "uniform sampler2D uTexture;\n" +
                    "void main() {\n" +
                    "    highp vec2 uv = vTextureCo\n"+
                    /*//左右分三屏
                    "    if(uv.x <= 1.0 / 3.0) {\n"+
                    "        uv.x = uv.x + 1.0 / 3.0;\n"+
                    "    } else if(uv.x >= 2.0 / 3.0) {\n"+
                    "        uv.x = uv.x - 1.0 / 3.0;\n"+
                    "    }\n"+
                    //上下分两屏，保留0.25~0.75
                    "    if(uv.y <= 0.5) {\n"+
                    "        uv.y = uv.y + 0.25;\n"+
                    "    } else {\n"+
                    "        uv.y = uv.y - 0.25;\n"+
                    "    }\n"+*/
                    "    gl_FragColor = texture2D( uTexture, vTextureCo);\n" +
                    "}";


    public SixScreenFilter() {
        super(null, vertexCode, fragmentCode);
    }

    @Override
    protected void onCreate() {
        super.onCreate();
    }
}
