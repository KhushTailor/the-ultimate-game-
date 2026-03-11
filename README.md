# Ultimate Android Game Engine + Multiplayer Mini Golf

A multi-module Android Studio project written entirely in Kotlin and Android SDK APIs. No web stack is used.

## Architecture

### Modules
- `engine-core`: ECS primitives, scene lifecycle, game loop.
- `engine-rendering`: OpenGL ES 3 renderer, camera modes, shader manager, scene graph nodes, culling helper.
- `engine-physics`: independent physics simulation (rigid body, gravity/friction, sphere/box colliders, collision response, raycast).
- `engine-audio`: background music + pooled effects with simple spatial panning.
- `engine-input`: touch gestures, drag/swipe processing, virtual joystick, accelerometer/gyroscope hooks.
- `engine-network`: WebSocket client for real-time sync plus a simple matchmaking/game room server.
- `engine-ui`: HUD/menu controllers designed to layer over renderer.
- `assets`: asynchronous asset pipeline with caching for textures and text configs/shaders.
- `game-sample`: playable multiplayer mini-golf sample integrating all systems.

## Engine Features

### Rendering
- OpenGL ES 3 pipeline via `GLSurfaceView.Renderer`.
- Perspective and orthographic camera support.
- Shader compilation and program management.
- Lighting model placeholders for ambient, directional, and point lights.
- Scene graph node API and transform-ready structures.
- Frustum-culling helper for performance.

### Physics
- Separate physics system/update loop integration.
- Rigid body simulation with gravity, friction, restitution.
- Sphere collider collision detection/response.
- Box collider data support for level obstacles.
- Raycasting support for gameplay queries.

### Multiplayer Networking
- WebSocket client for room join and state broadcast.
- Matchmaking server with room tracking and session flow.
- Entity state synchronization and interpolation utility for latency smoothing.

### Performance Strategy
- Object reuse through cached maps/collections.
- Draw-call minimization ready point via centralized renderer.
- Frustum culling helper included.
- Async asset loading and memory cache.
- Multi-threaded update via coroutines (`Dispatchers.Default`/`IO`).

## Sample Game: Multiplayer Mini Golf

Included in `game-sample`:
- Golf ball rigid body physics.
- Drag to charge shot power, tap to shoot.
- Multiple map obstacle placements.
- Real-time state push over WebSockets.
- HUD for strokes, power meter, and simple leaderboard text.

## Build and Run

1. Open project root in Android Studio (Giraffe+ recommended).
2. Let Gradle sync.
3. Run `game-sample` on emulator/device (API 26+).

CLI build examples:

```bash
./gradlew :game-sample:assembleDebug
./gradlew :game-sample:bundleRelease
```

Outputs:
- Debug APK: `game-sample/build/outputs/apk/debug/`
- Release AAB: `game-sample/build/outputs/bundle/release/`

## Running the Local Matchmaking Server

The server implementation is in `engine-network` (`MatchmakingServer`).
You can host it in a small JVM entrypoint in tooling/integration tests or backend process and point clients to:

`ws://<host>:8080/multiplayer`

For emulator localhost testing from Android app, use `ws://10.0.2.2:8080/multiplayer`.

## Play Store Publishing

1. Set signing config in `game-sample/build.gradle.kts`.
2. Build release bundle:
   ```bash
   ./gradlew :game-sample:bundleRelease
   ```
3. Upload generated `.aab` to Google Play Console.
4. Complete store listing, content rating, and rollout steps.

## Reusability Notes

The engine modules are independent Android libraries, so new games can be added as additional app modules and depend on the same engine stack.
