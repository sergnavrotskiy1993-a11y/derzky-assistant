package ru.sergnavrotskiy.derzkyassistant

/**
 * Базовые границы детского ассистента.
 *
 * На первом этапе ассистент только принимает речь/текст и отвечает.
 * Любые действия с устройством должны оставаться недоступными.
 */
enum class ChildAssistantCapability {
    LISTEN,
    ANSWER,
    GIVE_ADVICE,
    DOWNLOAD_FILES,
    CHANGE_DEVICE_SETTINGS,
    MAKE_PURCHASES,
    SEND_MESSAGES,
    OPEN_EXTERNAL_APPS,
}

object ChildAssistantPolicy {
    private val allowedCapabilities = setOf(
        ChildAssistantCapability.LISTEN,
        ChildAssistantCapability.ANSWER,
        ChildAssistantCapability.GIVE_ADVICE,
    )

    fun isAllowed(capability: ChildAssistantCapability): Boolean {
        return capability in allowedCapabilities
    }

    fun blockedActionReply(): String {
        return "Я пока не могу выполнять действия на устройстве. " +
            "Могу объяснить, что можно сделать, или помочь попросить взрослого."
    }
}
