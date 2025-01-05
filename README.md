![Aspecto Graphics](https://github.com/user-attachments/assets/27b8e750-cc42-4e99-b8ff-ea549f118aa5)<br><br>

[Aspecto](https://github.com/vipulasri/aspecto) is a grid layout that perfectly preserves each item's aspect ratio for the [Jetpack Compose](https://developer.android.com/compose) / [Compose Multiplatform](https://www.jetbrains.com/compose-multiplatform/).

✨ Just as the 🪄 Patronus charm protects its caster, Aspecto protects image layouts from distortion!<br>

## Sample
[Android](https://github.com/vipulasri/aspecto/tree/main/sample-android) & 
[iOS](https://github.com/vipulasri/aspecto/tree/main/sample-ios) samples.
Both samples use the same [shared code](https://github.com/vipulasri/aspecto/tree/main/sample/src/commonMain/kotlin/com/vipulasri/aspecto/sample/App.kt) to demonstrate the Compose Multiplatform capabilities.

https://github.com/user-attachments/assets/58cd864b-0a82-431b-ac6e-de755f885b98

## Features

- **Aspect Ratio Preservation** - Maintains natural dimensions of images
- **Smart Row Distribution** - Magically arranges items for optimal space utilization
- ️**Efficient Updates** - Uses incremental calculations for smooth performance
- **Height Protection** - Guards against overly tall items disrupting layouts
- **Responsive Layout** - Adapts gracefully to different screen sizes

### Multiplatform Support
- Android
- iOS

## Installation

Add the dependency to your project:

### Compose Multiplatform
```kotlin
dependencies {
    implementation("com.vipulasri.aspecto:aspecto:<latest-version>")
}
```

### Android Only
```groovy
dependencies {
    implementation("com.vipulasri.aspecto:aspecto-android:<latest-version>")
}
```

## Usage

```kotlin
AspectoGrid(
    modifier = Modifier.fillMaxWidth(),
    contentPadding = PaddingValues(16.dp)
) {
    items(
        items = images,
        key = { it.id },
        aspectRatio = { it.width.toFloat() / it.height }
    ) { image ->
        // content
    }
}
```
See the [sample](https://github.com/vipulasri/aspecto/blob/main/sample/src/commonMain/kotlin/com/vipulasri/aspecto/sample/App.kt) application for more usage.

## Inspired by

* [Greedo Layout](https://github.com/500px/greedo-layout-for-android) from [500px](https://github.com/500px)

## License

```
Copyright 2025 Vipul Asri

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```