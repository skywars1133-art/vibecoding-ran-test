package com.chesslock.ui.puzzle

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.util.Log
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import com.caverock.androidsvg.SVG
import com.chesslock.R
import com.chesslock.chess.ChessPieceType
import com.chesslock.data.PieceStyle

/** The glyph drawn for this piece type in the selected text [style], or null for artwork sets. */
fun ChessPieceType.glyph(style: PieceStyle): String? = when (style) {
    PieceStyle.CLASSIC -> when (this) {
        ChessPieceType.KING -> "♚"
        ChessPieceType.QUEEN -> "♛"
        ChessPieceType.ROOK -> "♜"
        ChessPieceType.BISHOP -> "♝"
        ChessPieceType.KNIGHT -> "♞"
        ChessPieceType.PAWN -> "♟"
    }

    PieceStyle.OUTLINE -> when (this) {
        ChessPieceType.KING -> "♔"
        ChessPieceType.QUEEN -> "♕"
        ChessPieceType.ROOK -> "♖"
        ChessPieceType.BISHOP -> "♗"
        ChessPieceType.KNIGHT -> "♘"
        ChessPieceType.PAWN -> "♙"
    }

    PieceStyle.LETTERS -> when (this) {
        ChessPieceType.KING -> "K"
        ChessPieceType.QUEEN -> "Q"
        ChessPieceType.ROOK -> "R"
        ChessPieceType.BISHOP -> "B"
        ChessPieceType.KNIGHT -> "N"
        ChessPieceType.PAWN -> "P"
    }

    PieceStyle.CBURNETT,
    PieceStyle.CHESSNUT,
    PieceStyle.FANTASY,
    PieceStyle.CELTIC,
    PieceStyle.SPATIAL,
    -> null
}

/** Bundled artwork drawable for this piece in [style], or null when the style draws text glyphs. */
fun PieceStyle.artwork(type: ChessPieceType, isWhite: Boolean): Int? = when (this) {
    PieceStyle.CBURNETT -> when (type) {
        ChessPieceType.KING -> if (isWhite) R.drawable.cburnett_wk else R.drawable.cburnett_bk
        ChessPieceType.QUEEN -> if (isWhite) R.drawable.cburnett_wq else R.drawable.cburnett_bq
        ChessPieceType.ROOK -> if (isWhite) R.drawable.cburnett_wr else R.drawable.cburnett_br
        ChessPieceType.BISHOP -> if (isWhite) R.drawable.cburnett_wb else R.drawable.cburnett_bb
        ChessPieceType.KNIGHT -> if (isWhite) R.drawable.cburnett_wn else R.drawable.cburnett_bn
        ChessPieceType.PAWN -> if (isWhite) R.drawable.cburnett_wp else R.drawable.cburnett_bp
    }

    PieceStyle.CLASSIC,
    PieceStyle.OUTLINE,
    PieceStyle.LETTERS,
    PieceStyle.CHESSNUT,
    PieceStyle.FANTASY,
    PieceStyle.CELTIC,
    PieceStyle.SPATIAL,
    -> null
}

/**
 * Renders the bundled licensed SVG sets with AndroidSVG. The board asks for each piece many times
 * per frame and across recompositions, so the small rendered bitmaps are cached by asset key.
 */
private object SvgPieceRenderer {
    private const val BITMAP_SIZE_PX = 192
    private const val CACHE_ENTRIES = 64

    private val cache = LruCache<String, ImageBitmap>(CACHE_ENTRIES)

    fun render(
        context: Context,
        folder: String,
        type: ChessPieceType,
        isWhite: Boolean,
    ): ImageBitmap? {
        val file = "${if (isWhite) "w" else "b"}${type.assetLetter()}"
        val key = "$folder/$file"
        cache.get(key)?.let { return it }
        return runCatching {
            context.assets.open("pieces/$folder/$file.svg").use { stream ->
                val svg = SVG.getFromInputStream(stream)
                val picture = svg.renderToPicture(BITMAP_SIZE_PX, BITMAP_SIZE_PX)
                val bitmap = Bitmap.createBitmap(
                    BITMAP_SIZE_PX,
                    BITMAP_SIZE_PX,
                    Bitmap.Config.ARGB_8888,
                )
                Canvas(bitmap).drawPicture(picture)
                bitmap.asImageBitmap()
            }
        }.onFailure { error ->
            Log.w("PieceStyle", "Could not render SVG piece $key", error)
        }.getOrNull()?.also { cache.put(key, it) }
    }

    private fun ChessPieceType.assetLetter(): Char = when (this) {
        ChessPieceType.KING -> 'K'
        ChessPieceType.QUEEN -> 'Q'
        ChessPieceType.ROOK -> 'R'
        ChessPieceType.BISHOP -> 'B'
        ChessPieceType.KNIGHT -> 'N'
        ChessPieceType.PAWN -> 'P'
    }
}

@Composable
fun PieceGlyph(
    type: ChessPieceType,
    isWhite: Boolean,
    style: PieceStyle,
    fontSize: TextUnit,
    modifier: Modifier = Modifier,
) {
    val artwork = style.artwork(type, isWhite)
    if (artwork != null) {
        Image(
            painter = painterResource(artwork),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = modifier.fillMaxSize(),
        )
        return
    }

    val svgFolder = style.svgFolder
    if (svgFolder != null) {
        val context = LocalContext.current
        val bitmap = remember(svgFolder, type, isWhite) {
            SvgPieceRenderer.render(context, svgFolder, type, isWhite)
        }
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = modifier.fillMaxSize(),
            )
            return
        }
    }

    // Artwork that failed to load still leaves a playable board by falling back to Classic glyphs.
    val glyphStyle = if (style.isText) style else PieceStyle.CLASSIC
    val glyph = type.glyph(glyphStyle) ?: return
    val fill = if (isWhite) Color.White else Color(0xFF171717)
    val contrast = if (isWhite) Color(0xFF171717) else Color.White
    val strokeWidth = with(LocalDensity.current) { 1.5.dp.toPx() }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        if (glyphStyle == PieceStyle.OUTLINE) {
            // The Unicode outline glyphs are hollow; stroke them so white pieces stay
            // visible on light squares and black pieces on dark squares.
            Text(
                text = glyph,
                color = contrast,
                fontSize = fontSize,
                maxLines = 1,
                softWrap = false,
                style = TextStyle(drawStyle = Stroke(width = strokeWidth)),
            )
        }
        Text(
            text = glyph,
            color = fill,
            fontSize = fontSize,
            maxLines = 1,
            softWrap = false,
            style = TextStyle(
                fontWeight = if (glyphStyle == PieceStyle.LETTERS) FontWeight.Bold else FontWeight.Normal,
                shadow = Shadow(color = contrast.copy(alpha = 0.65f), blurRadius = 3f),
            ),
        )
    }
}

@Composable
fun PieceStylePreview(
    style: PieceStyle,
    fontSize: TextUnit,
    modifier: Modifier = Modifier,
) {
    val pieces = listOf(
        ChessPieceType.KING to true,
        ChessPieceType.QUEEN to false,
        ChessPieceType.ROOK to true,
        ChessPieceType.BISHOP to false,
        ChessPieceType.KNIGHT to true,
        ChessPieceType.PAWN to false,
    )
    val itemSize = with(LocalDensity.current) { fontSize.toDp() } * 1.2f
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        pieces.forEach { (type, isWhite) ->
            Box(modifier = Modifier.size(itemSize), contentAlignment = Alignment.Center) {
                PieceGlyph(type = type, isWhite = isWhite, style = style, fontSize = fontSize)
            }
        }
    }
}
