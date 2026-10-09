package com.consay.app.data

import com.consay.app.model.InterventionMessage

object MessageRepository {
    private val messages = listOf(
        InterventionMessage(1, "Mai tỉnh rượu rồi nhắn, giờ thì nghỉ nhé!"),
        InterventionMessage(2, "Bạn đã đặt luật lúc còn tỉnh táo đấy."),
        InterventionMessage(3, "Một tin nhắn mất 5 giây, một đêm trằn trọc mất lâu hơn."),
        InterventionMessage(4, "Bình tĩnh, người ta chưa chắc đang chờ tin nhắn của bạn đâu."),
        InterventionMessage(5, "Cơn Say không phán xét. Cơn Say chỉ nhắc lời hứa thôi."),
        InterventionMessage(6, "Uống thêm ngụm nước lọc đi bạn ơi, điện thoại để đó mai tính."),
        InterventionMessage(7, "Lời hứa lúc 19:00 có giá trị hơn cảm xúc bốc đồng lúc nửa đêm!"),
        InterventionMessage(8, "Nhắn xong mai tỉnh dậy lại ôm đầu hối hận đấy, quay lại nào!")
    )

    fun getRandomMessage(): InterventionMessage {
        return messages.random()
    }

    fun getAllMessages(): List<InterventionMessage> {
        return messages
    }
}
