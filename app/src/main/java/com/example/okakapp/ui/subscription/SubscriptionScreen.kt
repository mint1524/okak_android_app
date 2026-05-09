package com.example.okakapp.ui.subscription

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.okakapp.data.remote.PlanDto

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionScreen(
    onBack: () -> Unit,
    vm: SubscriptionViewModel = viewModel(factory = SubscriptionViewModel.Factory)
) {
    val state by vm.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Подписка") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier
            .fillMaxSize()
            .padding(padding)
        ) {
            if (state.isLoading) {
                Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    val s = state.status
                    if (s != null && s.status == "active") {
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Текущая подписка: ${s.plan ?: "-"}",
                                    style = MaterialTheme.typography.titleMedium)
                                Text("Действует до: ${s.expiresAt?.take(10) ?: "-"}")
                                Text("Запросов: ${s.requestsUsed ?: 0} / ${s.requestLimit ?: 0}")
                                Text("Токенов: ${s.tokensUsed ?: 0} / ${s.tokenLimit ?: 0}")
                            }
                        }
                    } else {
                        Text("У вас нет активной подписки", style = MaterialTheme.typography.titleMedium)
                    }

                    Text("Доступные тарифы", style = MaterialTheme.typography.titleMedium)
                    state.plans.forEach { plan ->
                        PlanCard(plan = plan, isPurchasing = state.isPurchasing) { vm.buy(plan) }
                    }

                    state.message?.let {
                        Text(it, color = MaterialTheme.colorScheme.primary)
                    }
                    state.error?.let {
                        Text(it, color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}

@Composable
private fun PlanCard(plan: PlanDto, isPurchasing: Boolean, onBuy: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(plan.name, style = MaterialTheme.typography.titleLarge)
            Text("${plan.price} ₽ в месяц")
            Text("${plan.requestLimit} запросов, ${plan.tokenLimit} токенов")
            Button(
                onClick = onBuy,
                enabled = !isPurchasing,
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Text(if (isPurchasing) "..." else "Оформить")
            }
        }
    }
}
