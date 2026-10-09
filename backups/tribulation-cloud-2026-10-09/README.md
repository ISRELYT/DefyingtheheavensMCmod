# Tribulation cloud backup (2026-10-09)

A copy of everything that makes up the Heavenly Tribulation cloud, taken before changes. Where each file lives:

| File | Original location |
| --- | --- |
| TribulationCloud.java | src/main/java/com/example/defyingtheheavens/ (the cloud entity) |
| TribulationCloudRenderer.java | src/client/java/com/example/defyingtheheavens/client/ (draws the cloud) |
| tribulation_cloud.png | src/main/resources/assets/defying-the-heavens/textures/entity/ |
| ModEntities.java | src/main/java/com/example/defyingtheheavens/ (registers the cloud and lightning entities) |
| TribulationManager.java | src/main/java/com/example/defyingtheheavens/ (spawns, moves and removes the cloud; strikes) |
| TribulationLightning.java | src/main/java/com/example/defyingtheheavens/ (the bolts from the cloud) |
| TribulationLightningRenderer.java | src/client/java/com/example/defyingtheheavens/client/ |
| TribulationAtmosphere.java | src/client/java/com/example/defyingtheheavens/client/ (sky/fog darkening under the cloud) |
| ClientLevelSkyMixin.java | src/client/java/com/example/defyingtheheavens/client/mixin/ (tribulation sky and cloud colour) |
| TribulationHud.java, ClientTribulationData.java | src/client/java/com/example/defyingtheheavens/client/ (strikes-left HUD) |
| TribulationCloudGameTests.java | src/gametest/java/com/example/defyingtheheavens/ |

Renderers are registered in DefyingTheHeavensClient.java:

    EntityRendererRegistry.register(ModEntities.TRIBULATION_LIGHTNING, TribulationLightningRenderer::new);
    EntityRendererRegistry.register(ModEntities.TRIBULATION_CLOUD, TribulationCloudRenderer::new);
    TribulationHud.register();
    TribulationAtmosphere.register();
