package voodoo.data.curse

enum class DependencyType {
    // Token: 0x04000055 RID: 85
    EmbeddedLibrary,
    // Token: 0x04000056 RID: 86
    OptionalDependency,
    // Token: 0x04000057 RID: 87
    RequiredDependency,
    // Token: 0x04000058 RID: 88
    Tool,
    // Token: 0x04000059 RID: 89
    Incompatible,
    // Token: 0x0400005A RID: 90
    Include;
}