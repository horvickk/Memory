package com.horvickk.memory.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.horvickk.memory.ui.theme.LocalAppColors
import com.horvickk.memory.ui.theme.Type

/** Кнопка-иконка 48×48. */
@Composable
fun IconTap(kind: IconKind, description: String, modifier: Modifier = Modifier, size: Int = 30, onClick: () -> Unit) {
    Box(
        modifier
            .size(48.dp)
            .semantics {
                contentDescription = description
                role = Role.Button
            }
            .tap(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        LineIcon(kind, size = size.dp)
    }
}

/** Верхняя панель с заголовком по центру (главный экран). */
@Composable
fun CenterTopBar(title: String, left: @Composable () -> Unit, right: @Composable () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(64.dp)
            .padding(horizontal = 12.dp),
    ) {
        Box(Modifier.align(Alignment.CenterStart)) { left() }
        Txt(title, Type.screenTitle, Modifier.align(Alignment.Center))
        Box(Modifier.align(Alignment.CenterEnd)) { right() }
    }
}

/** Верхняя панель: закрыть/назад, заголовок слева, справа — дополнительные элементы. */
@Composable
fun StartTopBar(
    title: String,
    navIcon: IconKind,
    onNav: () -> Unit,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(64.dp)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconTap(navIcon, if (navIcon == IconKind.CLOSE) "Закрыть" else "Назад", onClick = onNav)
        Spacer(Modifier.width(4.dp))
        Txt(
            title,
            Type.screenTitle,
            Modifier.weight(1f),
            maxLines = 1,
            autoSize = TextAutoSize.StepBased(minFontSize = 13.sp, maxFontSize = 23.sp, stepSize = 0.5.sp),
        )
        actions()
    }
}

/** Диалог в стиле приложения: рамка, текст, кнопки. */
@Composable
fun InkDialog(
    title: String,
    text: String,
    confirm: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    dismiss: String? = null,
) {
    val colors = LocalAppColors.current
    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .fillMaxWidth()
                .background(colors.paper)
                .inkBorder()
                .padding(20.dp),
        ) {
            Txt(title, Type.cardTitle)
            Spacer(Modifier.height(12.dp))
            Txt(text, Type.body, color = colors.inkSoft)
            Spacer(Modifier.height(20.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (dismiss != null) {
                    InkButton(dismiss, Modifier.weight(1f), onClick = onDismiss)
                }
                InkButton(confirm, Modifier.weight(1f), onClick = onConfirm)
            }
        }
    }
}

/** Статус игры в правой части панели: текст и под ним сердечки-жизни. */
@Composable
fun StatusView(text: String, lives: Int?, maxLives: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Txt(text, Type.button.copy(fontSize = 17.sp), align = TextAlign.Center, maxLines = 1)
        if (lives != null) {
            Spacer(Modifier.height(2.dp))
            Row {
                repeat(maxLives) { i ->
                    LineIcon(if (i < lives) IconKind.HEART else IconKind.HEART_EMPTY, size = 15.dp)
                }
            }
        }
    }
}
