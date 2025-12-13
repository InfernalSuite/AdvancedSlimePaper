plugins {
    id("asp.base-conventions")
    id("asp.publishing-conventions")
}

dependencies {
    compileOnly(project(":api"))

    api(project(":loaders:api-loader"))
    api(project(":loaders:file-loader"))
    api(project(":loaders:mongo-loader"))
    api(project(":loaders:mysql-loader"))
    api(project(":loaders:redis-loader"))
    api(project(":loaders:postgresql-loader"))

    compileOnly(paperApi())
}

publishConfiguration {
    name = "Advanced Slime Paper Loaders"
    description = "Default loaders for Advanced Slime Paper. There might be more loaders available then included in this BOM package"
}
