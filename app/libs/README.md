# Vendored libsu artifacts

The `libsu` 5.0.3 and `sardine-android` 0.9 AARs are vendored because their
upstreams publish them through JitPack only, while this project intentionally
does not use the JitPack repository. They are built from the official
`topjohnwu/libsu` 5.0.3 and `thegrizzlylabs/sardine-android` 0.9 tags.

SHA-256 checksums are recorded in `checksums.sha256`. When updating libsu,
replace the related AARs together and update that file.
