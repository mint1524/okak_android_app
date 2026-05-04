# okak android

Compose-клиент к [okak backend](https://github.com/mint1524/okak_android_backend).

## Сборка

```bash
./gradlew :app:assembleDebug
```

## Запуск на эмуляторе

1. Запустить бэкенд (`docker compose up -d` в репозитории бэкенда).
2. Адрес бэкенда для эмулятора (`http://10.0.2.2:8080`) задан в `data/remote/ApiClient.kt`.
   Cleartext-трафик разрешён только для `10.0.2.2` и `localhost` в `network_security_config.xml`.
3. `./gradlew installDebug` или запуск из Android Studio.

## Запуск на реальном устройстве

В `ApiClient.BASE_URL` указывается адрес задеплоенного бэкенда (например `https://okak.example.com`).
HTTPS работает без дополнительной настройки.

## Структура

```
data/
  remote/        # Retrofit + DTO + auth-interceptor
  local/         # DataStore (token)
  repository/    # обёртки над Retrofit с маппингом ошибок
ui/
  auth/          # Login, Register
  chat/          # ChatsList, Chat
  subscription/  # план и покупка
  profile/       # email, лимиты, выход
```

## Стек

- Kotlin 2.0, Compose Material3
- Retrofit 2.11 + OkHttp 4.12 + kotlinx-serialization
- DataStore Preferences
- Navigation Compose, ViewModel + StateFlow
