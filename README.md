# Blade Vue Connector

<!-- Plugin description -->
Connects Vue components used in Laravel Blade views to their `.vue` files.

For Vue apps mounted on Blade layouts, where component tags such as `<text-input>` are written directly
in Blade views:

- **Go to declaration:** Ctrl+click a component tag to open its `.vue` file.
- **Component tags are recognised:** they are coloured (Settings | Editor | Color Scheme | Blade Vue
  Connector) and no longer reported as unknown HTML tags.
- **Unregistered components:** an inspection reports tags that look like components but aren't
  registered in any Vue app.
- **Find Usages** on a `.vue` file lists the Blade views that use it, under every name it is registered
  as.

Components are found by following registrations in your JavaScript and TypeScript files:
`app.component(...)`, `Vue.component(...)` and `components: {}` on `createApp(...)` / `new Vue(...)`,
including `require(...).default`, `defineAsyncComponent(...)` and dynamic `import(...)`. A tag that
isn't registered falls back to a `.vue` file with the same name.
<!-- Plugin description end -->

## Requirements

PhpStorm 2025.3 or newer, with bundled Blade, JavaScript and Vue.js plugins enabled.

## Limitations

- Components registered dynamically (for example with a library's exports, `import.meta.glob` or
  `require.context`) can't be resolved. When a project has such registrations, the unregistered-tag
  inspection stays quiet unless set otherwise in its options.
- Registrations inside `<script>` blocks in Blade views are not detected.
- Tag matching ignores case and hyphens, so `<AlertBanner>` in a Blade view is treated as registered
  even though the browser lowercases it before Vue parses it.

## Development

```shell
./gradlew test           # tests against PhpStorm 2026.2
./gradlew verifyPlugin   # Plugin Verifier: PhpStorm 2025.3, 2026.1, 2026.2
./gradlew buildPlugin    # build/distributions/*.zip
./gradlew runIde         # a sandboxed PhpStorm with the plugin installed
```

Add `-PlocalIdePath=/snap/phpstorm/current` (or any PhpStorm installation) to use a local IDE instead
of downloading PhpStorm 2026.2.
