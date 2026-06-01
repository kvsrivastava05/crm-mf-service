package com.example.mfservice.domain

enum class FundCategory { EQUITY, DEBT, HYBRID, LIQUID, ELSS }

enum class SipStatus { ACTIVE, PAUSED, CANCELLED }

enum class OrderStatus { CURRENT, PAST, CANCELLED }

enum class OrderType { PURCHASE, REDEMPTION, SIP, SWITCH }

enum class TxnType { PURCHASE, REDEMPTION, DIVIDEND, SIP }
