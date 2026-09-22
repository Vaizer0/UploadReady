package com.vaizero.uploadready.ui.tools

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vaizero.uploadready.ui.components.RequirementBadge

@Composable
fun ToolsScreen(
    onNavigateToPhoto: () -> Unit,
    onNavigateToSignature: () -> Unit,
    onNavigateToPdf: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("tools_screen_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                Text(
                    text = "Application Tools",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Choose a utility to prepare your files to exact specifications",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Photo Tool Card
        item {
            ToolDetailCard(
                title = "Photo Tool",
                tagline = "Resize, crop & compress photos",
                description = "Set exact width & height in pixels (e.g., 200x230 px). Crop passport or exam ratios, and compress to target file size (e.g., 20–50 KB) without losing dimensions.",
                features = listOf("Exact Pixel Resizing", "Preset & Custom Crop", "Strict KB Compression", "JPEG / PNG Format"),
                buttonText = "Open Photo Tool",
                testTag = "tools_open_photo_button",
                onClick = onNavigateToPhoto
            )
        }

        // Signature Tool Card
        item {
            ToolDetailCard(
                title = "Signature Tool",
                tagline = "Clean white background & exact dimensions",
                description = "Crop unnecessary margins around signature ink, clean and whiten paper background shadows, and compress to exact target size (e.g., 10–20 KB).",
                features = listOf("Auto-Trim Ink Margins", "Clean White Background", "Exact Dimensions (e.g. 140x60)", "High-contrast Output"),
                buttonText = "Open Signature Tool",
                testTag = "tools_open_signature_button",
                onClick = onNavigateToSignature
            )
        }

        // PDF Tool Card
        item {
            ToolDetailCard(
                title = "PDF Tool",
                tagline = "Images to PDF & Document Merger",
                description = "Convert documents and certificates into standard A4 PDF files. Reorder pages easily and merge multiple PDFs into a single file under your form's file-size limit.",
                features = listOf("Images to A4 PDF", "Reorder Document Pages", "Native PDF Merger", "Max File Size Control"),
                buttonText = "Open PDF Tool",
                testTag = "tools_open_pdf_button",
                onClick = onNavigateToPdf
            )
        }
    }
}

@Composable
fun ToolDetailCard(
    title: String,
    tagline: String,
    description: String,
    features: List<String>,
    buttonText: String,
    testTag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = tagline,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Feature Badges
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                features.take(2).forEach { feat ->
                    RequirementBadge(text = feat)
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                features.drop(2).forEach { feat ->
                    RequirementBadge(text = feat)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(testTag),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(buttonText, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
