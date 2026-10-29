package app.plenty.ui.bag

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.plenty.R
import app.plenty.core.content.GameContent
import app.plenty.core.content.MaterialId
import app.plenty.ui.ContentRes
import app.plenty.ui.components.GroveButton
import app.plenty.ui.components.TraitBadge
import app.plenty.ui.components.TraitBadgeStyle
import app.plenty.ui.theme.PlentyPalette
import app.plenty.ui.theme.PlentyTheme

private val SheetShape = RoundedCornerShape(32.dp)

/**
 * 14a 재료 설명: read-only right sheet over the whole window. The scrim swallows touches so the bag
 * keeps its scroll and tab but cannot be operated; 확인, the close button and system back all dismiss.
 */
@Composable
fun MaterialSheet(material: MaterialId, owned: Int, content: GameContent, onDismiss: () -> Unit) {
    val colors = PlentyTheme.colors
    val type = PlentyTheme.typography
    val trait = content.traitBoostedBy(material)
    val name = stringResource(ContentRes.materialName(material))
    BackHandler(onBack = onDismiss)
    Box(
        Modifier
            .fillMaxSize()
            .background(PlentyPalette.Navy.copy(alpha = 0.22f))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = {}),
    ) {
        Column(
            Modifier
                .align(Alignment.CenterEnd)
                .padding(24.dp)
                .width(648.dp)
                .fillMaxHeight()
                .shadow(24.dp, SheetShape, ambientColor = colors.shadow, spotColor = colors.shadow)
                .clip(SheetShape)
                .background(colors.surface)
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = {})
                .semantics {
                    paneTitle = name
                    isTraversalGroup = true
                }
                .padding(horizontal = 40.dp, vertical = 24.dp),
        ) {
            Box(Modifier.fillMaxWidth()) {
                Box(
                    Modifier
                        .align(Alignment.TopEnd)
                        .size(64.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(colors.background)
                        .clickable(role = Role.Button, onClick = onDismiss),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(painterResource(R.drawable.ic_close), stringResource(R.string.close), tint = colors.primary, modifier = Modifier.size(24.dp))
                }
            }
            Spacer(Modifier.height(30.dp))
            Box(Modifier.size(100.dp).background(colors.surfaceTonal, RoundedCornerShape(28.dp)), contentAlignment = Alignment.Center) {
                Icon(painterResource(ContentRes.materialIcon(material)), null, tint = colors.accent, modifier = Modifier.size(44.dp))
            }
            Spacer(Modifier.height(36.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(name, style = type.title.copy(fontSize = 40.sp, lineHeight = 52.sp), color = colors.primary, modifier = Modifier.weight(1f))
                Text(
                    text = stringResource(R.string.sheet_owned, owned),
                    style = type.caption.copy(fontWeight = FontWeight.Medium),
                    color = colors.primary,
                    modifier = Modifier.background(colors.surfaceTonal, CircleShape).padding(horizontal = 20.dp, vertical = 10.dp),
                )
            }
            if (trait != null) {
                Spacer(Modifier.height(16.dp))
                TraitBadge(trait, TraitBadgeStyle.Default)
            }
            Spacer(Modifier.height(36.dp))
            Text(stringResource(ContentRes.materialFlavor(material)), style = type.heading.copy(fontSize = 27.sp, fontWeight = FontWeight.SemiBold), color = colors.primary)
            if (trait != null) {
                Spacer(Modifier.height(18.dp))
                Text(
                    stringResource(R.string.sheet_body, trait.label, content.growth.traitPointsPerMaterial),
                    style = type.body.copy(lineHeight = 40.sp),
                    color = colors.textSecondary,
                )
            }
            Spacer(Modifier.height(36.dp))
            Text(
                stringResource(R.string.sheet_info),
                style = type.caption.copy(fontSize = 21.sp, lineHeight = 34.sp),
                color = colors.textSecondary,
                modifier = Modifier.fillMaxWidth().background(colors.background, RoundedCornerShape(24.dp)).padding(24.dp),
            )
            Spacer(Modifier.weight(1f))
            GroveButton(stringResource(R.string.sheet_confirm), onDismiss, Modifier.fillMaxWidth(), trailingIcon = null)
        }
    }
}
