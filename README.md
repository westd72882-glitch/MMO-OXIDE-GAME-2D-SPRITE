# Wasteland — Android survival прототип

Kotlin + Jetpack Compose. Баланс монет, ресурсы (Сера, Камень), крафт, магазин
(покупка ресурсов и готовых предметов за монеты), инвентарь с drag&drop.

## Как это собрано (по образцу вашего build.yml)

Иконки лежат **прямо в корне репозитория** как обычные PNG:

```
Rocket.png
Stone.png
Sulfur_Ore.png
coin.png
Hunting_Rifle_Bullet.png
ic_placeholder.png
```

При каждой сборке `.github/workflows/build.yml`:
1. Берёт все `*.png` из корня репозитория
2. Приводит имя к нижнему регистру, заменяет непонятные символы на `_`
   (`Sulfur_Ore.png` → `sulfur_ore`, `Rocket.png` → `rocket`)
3. Копирует их в `app/src/main/res/drawable/`
4. Из `coin.png` (или `app_icon.png`, если добавите) генерирует иконку приложения
   всех размеров в `mipmap-*dpi`
5. Собирает debug APK и выкладывает как artifact `Wasteland-debug-apk`

Ничего вручную раскладывать по папкам не нужно — просто кладёте PNG в корень репо
и коммитите.

## Как добавить новую иконку/предмет

1. Кладёте `MyItem.png` в корень репозитория, коммитите
2. В `app/src/main/java/com/example/wasteland/GameData.kt` добавляете предмет
   с `iconRes = "myitem"` (имя в нижнем регистре, как после обработки workflow)
3. Всё — при следующей сборке иконка появится в игре

Если иконки для какого-то `iconRes` нет — сборка Android **не найдёт ресурс**
и упадёт с ошибкой, поэтому важно класть PNG для каждого `iconRes`, который
используете в коде.

## Что уже подключено

| Файл в репозитории | Drawable-имя (после санитайзера) | Где используется |
|---|---|---|
| `coin.png` | `coin` | Баланс игрока (иконка монет), иконка приложения |
| `Sulfur_Ore.png` | `sulfur_ore` | Ресурс «Сера» |
| `Stone.png` | `stone` | Ресурс «Камень», крафт «Заточенный камень» |
| `Metal_Fragment.png` | `metal_fragment` | Ресурс «Металлолом» |
| `Pickaxe_0.png` | `pickaxe_0` | Крафт «Кирка» |
| `Rocket.png` | `rocket` | Магазин: «Ракета РПГ» |
| `Hunting_Rifle_Bullet.png` | `hunting_rifle_bullet` | Магазин: «Патрон охотничьей винтовки» |
| `9mm_Smg.png` | `res_9mm_smg` (имя не может начинаться с цифры) | Магазин: «9мм ПП» |
| `Assault_Rifle.png` | `assault_rifle` | Магазин: «Штурмовая винтовка» |
| `mining_quarry.png` | `mining_quarry` | Магазин: «Добывающая вышка» |
| `copter.png` | `copter` | Магазин: «Мини-вертолёт» |
| `Candy_Cane.png` | `candy_cane` | Магазин: «Леденец-трость» |

**Важно про имена файлов**: workflow приводит их к нижнему регистру и заменяет всё,
кроме `a-z0-9`, на `_`. Если итоговое имя начинается с цифры (как `9mm_Smg` → `9mm_smg`),
Android не разрешит такой идентификатор ресурса — workflow автоматически добавляет
префикс `res_` в этом случае (`res_9mm_smg`). Именно поэтому в `GameData.kt` для
`9mm_Smg.png` указано `iconRes = "res_9mm_smg"`, а не `"9mm_smg"`.

## Сборка

Пуш в `main` → GitHub Actions → Artifacts → `Wasteland-debug-apk`.

Локально: `./gradlew assembleDebug`, APK в `app/build/outputs/apk/debug/app-debug.apk`
(предварительно нужно вручную прогнать копирование иконок либо просто использовать CI).

## Структура кода

- `GameData.kt` — ресурсы, крафт-предметы, товары магазина, привязка к именам иконок
- `GameState.kt` — состояние игрока и вся игровая логика
- `InventoryGrid.kt` — сетка инвентаря с drag&drop
- `GameScreen.kt` — экран: шапка, ресурсы, вкладки
- `Components.kt` — переиспользуемые UI элементы
- `IconResolver.kt` — поиск drawable по имени с фоллбэком на заглушку
