package com.github.deianvn.compose.director.processor


import com.google.devtools.ksp.processing.*

class PropProcessorProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
        return PropProcessor(
            environment.codeGenerator,
            environment.logger
        )
    }
}
