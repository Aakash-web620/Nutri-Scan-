package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.theme.Amber600
import com.example.ui.theme.Emerald100
import com.example.ui.theme.Emerald50
import com.example.ui.theme.Emerald600
import com.example.ui.theme.Emerald700
import com.example.ui.theme.Emerald900
import com.example.ui.theme.Neutral200
import com.example.ui.theme.Neutral500
import com.example.ui.theme.Rose600
import com.example.ui.theme.Teal600
import com.example.ui.viewmodel.NutriViewModel
import kotlinx.coroutines.launch

@Composable
fun ProfileScreen(
    viewModel: NutriViewModel,
    modifier: Modifier = Modifier
) {
    val profile by viewModel.userProfile.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var name by remember { mutableStateOf("Alex") }
    var ageStr by remember { mutableStateOf("28") }
    var gender by remember { mutableStateOf("Female") }
    var weightStr by remember { mutableStateOf("65") }
    var heightStr by remember { mutableStateOf("170") }
    var fitnessGoal by remember { mutableStateOf("Weight Loss") }
    var activityLevel by remember { mutableStateOf("Moderate") }

    LaunchedEffect(profile) {
        profile?.let { p ->
            name = p.name
            ageStr = p.age.toString()
            gender = p.gender
            weightStr = p.weightKg.toInt().toString()
            heightStr = p.heightCm.toInt().toString()
            fitnessGoal = p.fitnessGoal
            activityLevel = p.activityLevel
        }
    }

    // Dynamic Live Formula: Mifflin-St Jeor equation
    val weightKg = weightStr.toFloatOrNull() ?: 65f
    val heightCm = heightStr.toFloatOrNull() ?: 170f
    val age = ageStr.toIntOrNull() ?: 28

    val liveBmr by remember(weightKg, heightCm, age, gender) {
        derivedStateOf {
            val base = if (gender.equals("Male", ignoreCase = true)) {
                (10f * weightKg) + (6.25f * heightCm) - (5f * age) + 5
            } else {
                (10f * weightKg) + (6.25f * heightCm) - (5f * age) - 161
            }
            base.toInt().coerceAtLeast(1000)
        }
    }

    val liveTdee by remember(liveBmr, activityLevel) {
        derivedStateOf {
            val mult = when (activityLevel) {
                "Sedentary" -> 1.2f
                "Light" -> 1.375f
                "Moderate" -> 1.55f
                "Very Active" -> 1.725f
                else -> 1.4f
            }
            (liveBmr * mult).toInt()
        }
    }

    val liveTargets by remember(liveTdee, fitnessGoal) {
        derivedStateOf {
            when (fitnessGoal) {
                "Weight Loss" -> {
                    val cal = (liveTdee - 400).coerceAtLeast(1200)
                    val p = ((cal * 0.30f) / 4f).toInt()
                    val c = ((cal * 0.40f) / 4f).toInt()
                    val f = ((cal * 0.30f) / 9f).toInt()
                    Triple(cal, p, Pair(c, f))
                }
                "Muscle Gain" -> {
                    val cal = liveTdee + 350
                    val p = ((cal * 0.30f) / 4f).toInt()
                    val c = ((cal * 0.45f) / 4f).toInt()
                    val f = ((cal * 0.25f) / 9f).toInt()
                    Triple(cal, p, Pair(c, f))
                }
                else -> { // Maintenance
                    val cal = liveTdee
                    val p = ((cal * 0.25f) / 4f).toInt()
                    val c = ((cal * 0.50f) / 4f).toInt()
                    val f = ((cal * 0.25f) / 9f).toInt()
                    Triple(cal, p, Pair(c, f))
                }
            }
        }
    }

    val goals = listOf("Weight Loss", "Muscle Gain", "Maintenance")
    val activityLevels = listOf("Sedentary", "Light", "Moderate", "Very Active")
    val genders = listOf("Female", "Male", "Other")

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("profile_scroll_list"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Emerald100),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = Emerald700,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "User Profile & Goals",
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Custom metabolic targets computed using Mifflin-St Jeor formula.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Live Metabolic Target Card (Top Result Card)
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("live_metabolic_target_card"),
                    colors = CardDefaults.cardColors(containerColor = Emerald50),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, Emerald100)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Dynamic Target Summary",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = Emerald900
                            )
                            Surface(
                                color = Emerald100,
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = fitnessGoal,
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = Emerald700,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("BMR", style = MaterialTheme.typography.labelSmall, color = Neutral500)
                                Text("$liveBmr kcal", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = Emerald900)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("TDEE", style = MaterialTheme.typography.labelSmall, color = Neutral500)
                                Text("$liveTdee kcal", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = Emerald900)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Daily Target", style = MaterialTheme.typography.labelSmall, color = Neutral500)
                                Text("${liveTargets.first} kcal", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = Emerald600)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Macro targets
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Protein", style = MaterialTheme.typography.labelSmall, color = Teal600)
                                Text("${liveTargets.second}g", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = Teal600)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Carbs", style = MaterialTheme.typography.labelSmall, color = Amber600)
                                Text("${liveTargets.third.first}g", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = Amber600)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Fats", style = MaterialTheme.typography.labelSmall, color = Rose600)
                                Text("${liveTargets.third.second}g", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = Rose600)
                            }
                        }
                    }
                }
            }

            // User Inputs Form
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(20.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = "Personal Biometrics",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("Display Name") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Gender selection
                        Text("Biological Gender", style = MaterialTheme.typography.labelMedium)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            genders.forEach { g ->
                                FilterChip(
                                    selected = gender == g,
                                    onClick = { gender = g },
                                    label = { Text(g) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Emerald600,
                                        selectedLabelColor = Color.White
                                    )
                                )
                            }
                        }

                        // Age, Weight, Height
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(
                                value = ageStr,
                                onValueChange = { ageStr = it },
                                label = { Text("Age") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = weightStr,
                                onValueChange = { weightStr = it },
                                label = { Text("Weight (kg)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = heightStr,
                                onValueChange = { heightStr = it },
                                label = { Text("Height (cm)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f)
                            )
                        }

                        // Daily Fitness Goal
                        Text("Daily Fitness Goal", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(goals) { g ->
                                FilterChip(
                                    selected = fitnessGoal == g,
                                    onClick = { fitnessGoal = g },
                                    label = { Text(g) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Emerald600,
                                        selectedLabelColor = Color.White
                                    )
                                )
                            }
                        }

                        // Activity Level
                        Text("Activity Level", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(activityLevels) { act ->
                                FilterChip(
                                    selected = activityLevel == act,
                                    onClick = { activityLevel = act },
                                    label = { Text(act) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Emerald600,
                                        selectedLabelColor = Color.White
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // Save Targets Button
            item {
                Button(
                    onClick = {
                        viewModel.updateProfile(
                            name = name,
                            age = age,
                            gender = gender,
                            weightKg = weightKg,
                            heightCm = heightCm,
                            fitnessGoal = fitnessGoal,
                            activityLevel = activityLevel
                        )
                        scope.launch {
                            snackbarHostState.showSnackbar("Profile & daily targets updated successfully!")
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("save_profile_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = Emerald600),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(imageVector = Icons.Default.Save, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Apply & Sync with Dashboard",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 70.dp)
        )
    }
}
