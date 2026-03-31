package com.google.amara.chattab;

abstract class ChatItem {}

class MessageItem extends ChatItem {
    ChatMessage message;
}

class HeaderItem extends ChatItem {
    String text;
    String id;
}