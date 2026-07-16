package io.archlens.analyzer.core.model;

import java.util.List;

public record Inventory(
        List<ModuleInfo> modules,
        List<PackageInfo> packages,
        List<TypeInfo> types,
        List<EndpointInfo> endpoints,
        List<PersistenceInfo> persistence,
        List<MessagingInfo> messaging
) {
}
