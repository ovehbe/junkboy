package com.ovehbe.junkboy.llm

/** Picks the right judge for the model the user selected. */
object OpenRouterJudges {

    fun create(apiKey: String, model: String, kind: ModelKind, spamCut: Double): DetailedSpamJudge =
        when (kind) {
            ModelKind.DECISION -> OpenRouterDecisionsJudge(apiKey, model, spamCut)
            ModelKind.CHAT -> OpenRouterJudge(apiKey, model)
        }

    /**
     * The stored model and kind, or the default. Installs from before decision models existed stored only
     * a chat model id, so a stored id without a kind is a chat model.
     */
    fun resolve(storedModel: String?, storedKind: String?): Pair<String, ModelKind> {
        if (storedModel.isNullOrBlank()) return OpenRouterModels.DEFAULT_MODEL to OpenRouterModels.DEFAULT_KIND
        val kind = ModelKind.fromStored(storedKind)
            ?: if (storedModel == OpenRouterModels.DEFAULT_MODEL) OpenRouterModels.DEFAULT_KIND else ModelKind.CHAT
        return storedModel to kind
    }
}
