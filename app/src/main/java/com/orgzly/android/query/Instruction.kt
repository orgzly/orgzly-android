package com.orgzly.android.query

sealed class Instruction {
    class GroupBy() : Instruction()
    class UseRepeater() : Instruction()
    class IgnorePast() : Instruction()
    class OpenNoteIfOnlyResult() : Instruction()
}