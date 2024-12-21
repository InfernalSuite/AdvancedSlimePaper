package com.infernalsuite.asp.conventions

import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import javax.inject.Inject

open class PublishConfiguration @Inject constructor(objects: ObjectFactory) {

    companion object {
        internal fun Project.publishConfiguration(): PublishConfiguration {
            return extensions.create("publishConfiguration", PublishConfiguration::class.java)
        }
    }

    val name: Property<String> = objects.property(String::class.java)
    val description: Property<String> = objects.property(String::class.java)

}
