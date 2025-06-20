package com.github.deianvn.compose.director.processor


import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.*
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ksp.*


class PropProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger
) : SymbolProcessor {

    override fun process(resolver: Resolver): List<KSAnnotated> {

        val sceneType = resolver.getClassDeclarationByName(
            resolver.getKSNameFromString("com.github.deianvn.compose.director.Scene")
        )?.asStarProjectedType()

        if (sceneType == null) {
            logger.warn("Type com.github.deianvn.compose.director.Scene")
            return emptyList()
        }

        val classes = resolver.getAllFiles()
            .flatMap { it.declarations }
            .filterIsInstance<KSClassDeclaration>()
            .filter { klass ->
                klass.asStarProjectedType().isAssignableFrom(sceneType)
            }

        for (klass in classes) {
            val className = klass.toClassName()

            val constructor = klass.primaryConstructor

            val fileSpecBuilder = FileSpec.builder(
                klass.packageName.asString(),
                "${className.simpleName}CopyPersistentGenerated"
            )

            val funBuilder = FunSpec.builder("copyPersistent")
                .receiver(className)
                .returns(className)

            if (constructor == null) {
                // No constructor -> copyPersistent returns this
                funBuilder.addStatement("return this")

                fileSpecBuilder.addFunction(funBuilder.build())

                val file = codeGenerator.createNewFile(
                    Dependencies(false, klass.containingFile ?: continue),
                    klass.packageName.asString(),
                    "${className.simpleName}CopyPersistentGenerated"
                )
                file.bufferedWriter().use { writer ->
                    fileSpecBuilder.build().writeTo(writer)
                }
                continue
            }

            val propsByName = klass.getAllProperties().associateBy { it.simpleName.asString() }

            // Check transient params have default values, collect persistent params
            val persistentParams = mutableListOf<KSValueParameter>()
            var errorFound = false

            for (param in constructor.parameters) {
                val paramName = param.name?.asString() ?: continue
                val prop = propsByName[paramName] ?: continue

                val persistent = prop.annotations
                    .firstOrNull { it.shortName.asString() == "Prop" }
                    ?.arguments
                    ?.firstOrNull { it.name?.asString() == "persistent" }
                    ?.value as? Boolean ?: true

                if (!persistent && !param.hasDefault) {
                    logger.error(
                        "Transient property '$paramName' must have a default value",
                        prop
                    )
                    errorFound = true
                }

                if (persistent) {
                    persistentParams.add(param)
                }
            }

            if (errorFound) {
                // Skip generating copyPersistent on error
                continue
            }

            if (constructor.parameters.isEmpty()) {
                // No params -> return this
                funBuilder.addStatement("return this")
            } else {
                // Generate call with persistent params only
                val paramAssignments = persistentParams.joinToString(", ") { param ->
                    val name = param.name!!.asString()
                    "$name = this.$name"
                }
                funBuilder.addStatement("return %T($paramAssignments)", className)
            }

            fileSpecBuilder.addFunction(funBuilder.build())

            val file = codeGenerator.createNewFile(
                Dependencies(false, klass.containingFile ?: continue),
                klass.packageName.asString(),
                "${className.simpleName}CopyPersistentGenerated"
            )
            file.bufferedWriter().use { writer ->
                fileSpecBuilder.build().writeTo(writer)
            }
        }

        return emptyList()
    }

    private fun generateCopyPersistent(classDecl: KSClassDeclaration) {
        val className = classDecl.simpleName.asString()
        val packageName = classDecl.packageName.asString()

        val persistentProps = classDecl.getAllProperties().filter { prop ->
            val propAnn = prop.annotations.find { it.shortName.asString() == "Prop" }
            when {
                propAnn == null -> false // default to transient
                else -> propAnn.arguments.any {
                    it.name?.asString() == "persistent" && it.value == true
                }
            }
        }

        val func = FunSpec.builder("nextScene")
            .receiver(classDecl.toClassName())
            .returns(classDecl.toClassName())
            .addStatement(
                "return %T(%L)",
                classDecl.toClassName(),
                persistentProps.joinToString(", ") {
                    "${it.simpleName.asString()} = this.${it.simpleName.asString()}"
                }
            )
            .build()

        val file = FileSpec.builder(packageName, "${className}_CopyGen")
            .addFunction(func)
            .build()

        file.writeTo(codeGenerator, Dependencies(false))
    }
}
