# AssentifySdk

[![Release](https://jitpack.io/v/AssentifyLTD/Assentify.SDK.Android.svg)](https://jitpack.io/#AssentifyLTD/Assentify.SDK.Android)
[![License](https://img.shields.io/github/license/AssentifyLTD/Assentify.SDK.Android)](https://github.com/AssentifyLTD/Assentify.SDK.Android/blob/main/LICENSE)
![Platform](https://img.shields.io/badge/platform-Android-green)

## Example

[To run the example project](https://github.com/AssentifyLTD/Assentify.Android.Demo)


## Documentation

[Assentify Sdk Documentation](https://drive.google.com/file/d/1LUrXUR3Bs8fvc169-t5c0uqbBT2-fYkZ/view?usp=drive_link)

## Installation

AssentifySdk is available through [JitPack](https://jitpack.io). To install

### Step 1: Add JitPack repository to your project-level `build.gradle` file

```gradle
allprojects {
    repositories {
        google()
        mavenCentral()
        // Add JitPack repository
        maven { url 'https://jitpack.io' }
    }
}
```

### Step 2. Add the dependency
```
dependencies {
    implementation 'com.github.AssentifyLTD:Assentify.SDK.Android:Tag'
}
```

## Versions

**0.0.62**
- Face liveness improvements
- Parameters change :onEnvironmentalConditionsChange 
- Parameters change :EnvironmentalConditions

**0.0.61**
- Face events improvements
- Face zoom improvements
- Face brightness improvements
- Face liveness improvements

**0.0.61-Beta**
- Face match improvements
- Face liveness improvements
- Flow Config

**0.0.61-Alpha**
- Flow Config

**0.0.60**
- Context Aware Signing Step

**0.0.59**
- Face match improvements

**0.0.58**
- Face match improvements

**0.0.57**
- Performance improvements

**0.0.56**
- Adding the face liveness check parameter
- Adding the Document liveness check parameter
- Enhance the translate feature
- Null check for the initialization parameters

**0.0.55**
- Handel the intermittent internet connectivity

**0.0.55-Alpha**
- Local face liveness check

**0.0.54**
- Matching The Templates With The Admin Portal Selected Templates

**0.0.53**
- Removing the Detect and Guide during the Transmitting process

**0.0.52**
- bug fixes and performance improvements

**0.0.50**
- Enhance The Image lossless compression Algorithm

**0.0.49**
- Image lossless compression

**0.0.48**
- bug fixes and performance improvements

**0.0.47**
- Face Match Countdown

**0.0.46**
- bug fixes and performance improvements

**0.0.45**
- Image Size : Improved image size .
- Templates Update: Change the Templates implementation from callback to normal function .
- Motion : Optimized motion handling for smoother performance during face and ID scanning.
- Memory Check: Check memory usage during scanning to prevent performance issues.
- Face Freeze Bug Fix: Resolved the issue where face freezing occurred during the face matching process.

**0.0.44**
- bug fixes and performance improvements

**0.0.43**
- bug fixes and performance improvements

## Author

Assentify, info.assentify@gmail.com

## License

AssentifySdk is available under the MIT license. See the LICENSE file for more info.
