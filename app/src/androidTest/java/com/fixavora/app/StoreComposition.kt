package com.fixavora.app

import android.graphics.*
import android.text.Layout
import android.text.StaticLayout
import android.text.TextDirectionHeuristics
import android.text.TextPaint
import android.text.TextUtils
import java.io.File
import java.util.Locale

/** Deterministic store artwork made from a real capture. Android shapes Arabic,
 * Indic and CJK text with the same platform font stack as the captured app. */
object StoreComposition {
    fun write(source: Bitmap, destination: File, headline: String, caption: String, locale: String, compact: Boolean) {
        val width=if(compact)800 else 1920
        val height=if(compact)1280 else 1200
        val margin=if(compact)38 else 80
        val titleSize=if(compact)35f else 54f
        val captionSize=if(compact)21f else 26f
        val bitmap=Bitmap.createBitmap(width,height,Bitmap.Config.ARGB_8888)
        val canvas=Canvas(bitmap); canvas.drawColor(Color.rgb(248,247,243))
        val rtl=Locale.forLanguageTag(locale).language=="ar"
        val svg=StringBuilder("""<svg xmlns="http://www.w3.org/2000/svg" width="$width" height="$height"><rect width="100%" height="100%" fill="#F8F7F3"/>""")
        fun text(value:String,size:Float,color:Int,y:Float,bold:Boolean):Float {
            val paint=TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                textSize=size; this.color=color; textLocale=Locale.forLanguageTag(locale)
                typeface=Typeface.create(if(bold)"sans-serif-medium" else "sans-serif",Typeface.NORMAL)
            }
            val layout=StaticLayout.Builder.obtain(value,0,value.length,paint,width-2*margin)
                .setAlignment(Layout.Alignment.ALIGN_NORMAL).setIncludePad(false)
                .setTextDirection(if(rtl)TextDirectionHeuristics.RTL else TextDirectionHeuristics.LTR).build()
            canvas.save();canvas.translate(margin.toFloat(),y);layout.draw(canvas);canvas.restore()
            for(line in 0 until layout.lineCount){
                val content=TextUtils.htmlEncode(value.substring(layout.getLineStart(line),layout.getLineEnd(line)).trim())
                svg.append("""<text x="${if(rtl)width-margin else margin}" y="${y+layout.getLineBaseline(line)}" font-family="sans-serif" font-size="$size" fill="${String.format("#%06X",color and 0xffffff)}" direction="${if(rtl)"rtl" else "ltr"}" text-anchor="${if(rtl)"end" else "start"}">$content</text>""")
            }
            return y+layout.height
        }
        var y=text(headline,titleSize,Color.rgb(23,42,58),30f,true)+12
        y=text(caption,captionSize,Color.rgb(36,107,115),y,false)+30
        val top=maxOf(if(compact)200f else 210f,y)
        require(top < height*0.40f){"Store headline/caption requires shortening for $locale"}
        val scale=minOf((width-2f*margin)/source.width,(height-top-40)/source.height)
        val outputWidth=source.width*scale;val outputHeight=source.height*scale
        val left=(width-outputWidth)/2
        canvas.drawRect(left-2,top-2,left+outputWidth+2,top+outputHeight+2,Paint().apply { color=Color.rgb(220,230,232) })
        canvas.drawBitmap(source,null,RectF(left,top,left+outputWidth,top+outputHeight),Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
        destination.parentFile!!.mkdirs()
        destination.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG,100,it) }
        svg.append("""<image x="$left" y="$top" width="$outputWidth" height="$outputHeight" href="../../raw/$locale/${if(compact)"compact" else "tablet"}/${destination.name}"/></svg>""")
        File(destination.parentFile,destination.nameWithoutExtension+".svg").writeText(svg.toString())
        bitmap.recycle()
    }
}
