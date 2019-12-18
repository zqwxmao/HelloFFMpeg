package com.michael.libplayer.opengl.filter;

import android.opengl.GLES11Ext;
import android.opengl.GLES20;

import com.michael.libplayer.opengl.base.BaseFilter;


/**
 * Oes纹理绘制滤镜
 */
public class OesFilter extends BaseFilter {

    private static final String vertexCode = "attribute vec4 aVertexCo;\n" +
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

    /*private static final String fragmentCode = "#extension GL_OES_EGL_image_external : require\n" +
            "precision highp float;\n" +
            "varying highp vec2 vTextureCo;\n" +
            "uniform samplerExternalOES uTexture;\n" +

            //
            "\n" +
            "    uniform highp vec2 singleStepOffset;\n" +
            "    uniform highp vec4 params;\n" +
            "    uniform highp float brightness;\n" +
            "    uniform float texelWidthOffset;\n"+
            "    uniform float texelHeightOffset;\n"+
            "\n" +
            "    const highp vec3 W = vec3(0.299, 0.587, 0.114);\n" +
            "    const highp mat3 saturateMatrix = mat3(\n" +
            "        1.1102, -0.0598, -0.061,\n" +
            "        -0.0774, 1.0826, -0.1186,\n" +
            "        -0.0228, -0.0228, 1.1772);\n" +
            "    highp vec2 blurCoordinates[24];\n" +
            "\n" +
            "    highp float hardLight(highp float color) {\n" +
            "    if (color <= 0.5)\n" +
            "        color = color * color * 2.0;\n" +
            "    else\n" +
            "        color = 1.0 - ((1.0 - color)*(1.0 - color) * 2.0);\n" +
            "    return color;\n" +
            "}\n" +
            "\n" +
            //

            "void main() {\n" +
            //

            "    highp vec3 centralColor = texture2D(uTexture, vTextureCo).rgb;\n" +
            "    vec2 singleStepOffset=vec2(texelWidthOffset,texelHeightOffset);\n"+
            "    blurCoordinates[0] = vTextureCo.xy + singleStepOffset * vec2(0.0, -10.0);\n" +
            "    blurCoordinates[1] = vTextureCo.xy + singleStepOffset * vec2(0.0, 10.0);\n" +
            "    blurCoordinates[2] = vTextureCo.xy + singleStepOffset * vec2(-10.0, 0.0);\n" +
            "    blurCoordinates[3] = vTextureCo.xy + singleStepOffset * vec2(10.0, 0.0);\n" +
            "    blurCoordinates[4] = vTextureCo.xy + singleStepOffset * vec2(5.0, -8.0);\n" +
            "    blurCoordinates[5] = vTextureCo.xy + singleStepOffset * vec2(5.0, 8.0);\n" +
            "    blurCoordinates[6] = vTextureCo.xy + singleStepOffset * vec2(-5.0, 8.0);\n" +
            "    blurCoordinates[7] = vTextureCo.xy + singleStepOffset * vec2(-5.0, -8.0);\n" +
            "    blurCoordinates[8] = vTextureCo.xy + singleStepOffset * vec2(8.0, -5.0);\n" +
            "    blurCoordinates[9] = vTextureCo.xy + singleStepOffset * vec2(8.0, 5.0);\n" +
            "    blurCoordinates[10] = vTextureCo.xy + singleStepOffset * vec2(-8.0, 5.0);\n" +
            "    blurCoordinates[11] = vTextureCo.xy + singleStepOffset * vec2(-8.0, -5.0);\n" +
            "    blurCoordinates[12] = vTextureCo.xy + singleStepOffset * vec2(0.0, -6.0);\n" +
            "    blurCoordinates[13] = vTextureCo.xy + singleStepOffset * vec2(0.0, 6.0);\n" +
            "    blurCoordinates[14] = vTextureCo.xy + singleStepOffset * vec2(6.0, 0.0);\n" +
            "    blurCoordinates[15] = vTextureCo.xy + singleStepOffset * vec2(-6.0, 0.0);\n" +
            "    blurCoordinates[16] = vTextureCo.xy + singleStepOffset * vec2(-4.0, -4.0);\n" +
            "    blurCoordinates[17] = vTextureCo.xy + singleStepOffset * vec2(-4.0, 4.0);\n" +
            "    blurCoordinates[18] = vTextureCo.xy + singleStepOffset * vec2(4.0, -4.0);\n" +
            "    blurCoordinates[19] = vTextureCo.xy + singleStepOffset * vec2(4.0, 4.0);\n" +
            "    blurCoordinates[20] = vTextureCo.xy + singleStepOffset * vec2(-2.0, -2.0);\n" +
            "    blurCoordinates[21] = vTextureCo.xy + singleStepOffset * vec2(-2.0, 2.0);\n" +
            "    blurCoordinates[22] = vTextureCo.xy + singleStepOffset * vec2(2.0, -2.0);\n" +
            "    blurCoordinates[23] = vTextureCo.xy + singleStepOffset * vec2(2.0, 2.0);\n" +
            "\n" +
            "    highp float sampleColor = centralColor.g * 22.0;\n" +
            "    sampleColor += texture2D(uTexture, blurCoordinates[0]).g;\n" +
            "    sampleColor += texture2D(uTexture, blurCoordinates[1]).g;\n" +
            "    sampleColor += texture2D(uTexture, blurCoordinates[2]).g;\n" +
            "    sampleColor += texture2D(uTexture, blurCoordinates[3]).g;\n" +
            "    sampleColor += texture2D(uTexture, blurCoordinates[4]).g;\n" +
            "    sampleColor += texture2D(uTexture, blurCoordinates[5]).g;\n" +
            "    sampleColor += texture2D(uTexture, blurCoordinates[6]).g;\n" +
            "    sampleColor += texture2D(uTexture, blurCoordinates[7]).g;\n" +
            "    sampleColor += texture2D(uTexture, blurCoordinates[8]).g;\n" +
            "    sampleColor += texture2D(uTexture, blurCoordinates[9]).g;\n" +
            "    sampleColor += texture2D(uTexture, blurCoordinates[10]).g;\n" +
            "    sampleColor += texture2D(uTexture, blurCoordinates[11]).g;\n" +
            "    sampleColor += texture2D(uTexture, blurCoordinates[12]).g * 2.0;\n" +
            "    sampleColor += texture2D(uTexture, blurCoordinates[13]).g * 2.0;\n" +
            "    sampleColor += texture2D(uTexture, blurCoordinates[14]).g * 2.0;\n" +
            "    sampleColor += texture2D(uTexture, blurCoordinates[15]).g * 2.0;\n" +
            "    sampleColor += texture2D(uTexture, blurCoordinates[16]).g * 2.0;\n" +
            "    sampleColor += texture2D(uTexture, blurCoordinates[17]).g * 2.0;\n" +
            "    sampleColor += texture2D(uTexture, blurCoordinates[18]).g * 2.0;\n" +
            "    sampleColor += texture2D(uTexture, blurCoordinates[19]).g * 2.0;\n" +
            "    sampleColor += texture2D(uTexture, blurCoordinates[20]).g * 3.0;\n" +
            "    sampleColor += texture2D(uTexture, blurCoordinates[21]).g * 3.0;\n" +
            "    sampleColor += texture2D(uTexture, blurCoordinates[22]).g * 3.0;\n" +
            "    sampleColor += texture2D(uTexture, blurCoordinates[23]).g * 3.0;\n" +
            "\n" +
            "    sampleColor = sampleColor / 62.0;\n" +
            "\n" +
            "    highp float highPass = centralColor.g - sampleColor + 0.5;\n" +
            "\n" +
            "    for (int i = 0; i < 5; i++) {\n" +
            "        highPass = hardLight(highPass);\n" +
            "    }\n" +
            "    highp float lumance = dot(centralColor, W);\n" +
            "\n" +
            "    highp float alpha = pow(lumance, params.r);\n" +
            "\n" +
            "    highp vec3 smoothColor = centralColor + (centralColor-vec3(highPass))*alpha*0.1;\n" +
            "\n" +
            "    smoothColor.r = clamp(pow(smoothColor.r, params.g), 0.0, 1.0);\n" +
            "    smoothColor.g = clamp(pow(smoothColor.g, params.g), 0.0, 1.0);\n" +
            "    smoothColor.b = clamp(pow(smoothColor.b, params.g), 0.0, 1.0);\n" +
            "\n" +
            "    highp vec3 lvse = vec3(1.0)-(vec3(1.0)-smoothColor)*(vec3(1.0)-centralColor);\n" +
            "    highp vec3 bianliang = max(smoothColor, centralColor);\n" +
            "    highp vec3 rouguang = 2.0*centralColor*smoothColor + centralColor*centralColor - 2.0*centralColor*centralColor*smoothColor;\n" +
            "\n" +
            "    gl_FragColor = vec4(mix(centralColor, lvse, alpha), 1.0);\n" +
            "    gl_FragColor.rgb = mix(gl_FragColor.rgb, bianliang, alpha);\n" +
            "    gl_FragColor.rgb = mix(gl_FragColor.rgb, rouguang, params.b);\n" +
            "\n" +
            "    highp vec3 satcolor = gl_FragColor.rgb * saturateMatrix;\n" +
            "    gl_FragColor.rgb = mix(gl_FragColor.rgb, satcolor, params.a);\n" +
            "    gl_FragColor.rgb = vec3(gl_FragColor.rgb + vec3(brightness));\n" +

            //
            "}";*/
    private static final String fragmentCode = "#extension GL_OES_EGL_image_external : require\n" +
            "precision highp float;\n" +
            "varying highp vec2 vTextureCo;\n" +
            "uniform samplerExternalOES uTexture;\n" +
            "void main() {\n" +
            "    highp vec2 ux = vTextureCo;\n"+
            //左右分三屏
            "    if(ux.x <= 1.0 / 3.0) {\n"+
            "        ux.x = ux.x + 1.0 / 3.0;\n"+
            "    } else if(ux.x >= 2.0 / 3.0) {\n"+
            "        ux.x = ux.x - 1.0 / 3.0;\n"+
            "    }\n"+
            //上下分两屏，保留0.25~0.75
            "    if(ux.y <= 0.5) {\n"+
            "        ux.y = ux.y + 0.25;\n"+
            "    } else {\n"+
            "        ux.y = ux.y - 0.25;\n"+
            "    }\n"+
            "    gl_FragColor = texture2D( uTexture, ux);\n" +
            "}";


    public OesFilter() {
        super(null, vertexCode, fragmentCode);
    }

    @Override
    protected void onBindTexture(int textureId) {
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId);
        GLES20.glUniform1i(mGLTexture, 0);
    }
}
