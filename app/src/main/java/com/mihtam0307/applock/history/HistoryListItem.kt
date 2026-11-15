package com.mihtam0307.applock.history

import com.mihtam0307.applock.data.AccessLogEntity

/** 1 dòng hiển thị trong danh sách lịch sử: tiêu đề ngày hoặc 1 lượt truy cập. */
sealed class HistoryListItem {
    data class DateHeader(val dateLabel: String) : HistoryListItem()
    data class Entry(val log: AccessLogEntity) : HistoryListItem()
}
