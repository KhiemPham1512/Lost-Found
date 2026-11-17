package com.lostfound.app.core.util

/** Câu báo lỗi hiển thị cho người dùng. */
fun Throwable.userMessage(): String =
    message?.takeIf { it.isNotBlank() } ?: "Có lỗi xảy ra, vui lòng thử lại"
