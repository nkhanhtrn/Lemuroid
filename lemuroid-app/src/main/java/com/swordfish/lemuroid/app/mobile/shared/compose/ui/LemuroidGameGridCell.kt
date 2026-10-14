package com.swordfish.lemuroid.app.mobile.shared.compose.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.swordfish.lemuroid.lib.library.db.entity.Game

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LemuroidGameGridCell(
    modifier: Modifier = Modifier,
    game: Game,
    onClick: () -> Unit = { },
    onLongClick: () -> Unit = { },
) {
    Column(
        modifier =
            modifier.combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
            ),
    ) {
        Box {
            LemuroidGameImage(
                modifier =
                    Modifier
                        .clip(RoundedCornerShape(12.dp)),
                game = game,
                contentScale = ContentScale.Fit,
            )
            if (game.isFavorite) {
                Icon(
                    imageVector = Icons.Filled.Star,
                    contentDescription = null,
                    tint = Color.Black,
                    modifier =
                        Modifier
                            .align(Alignment.TopEnd)
                            .padding(6.dp)
                            .background(Color(0x88FFFFFF), CircleShape)
                            .padding(2.dp),
                )
            }
        }
    }
}
