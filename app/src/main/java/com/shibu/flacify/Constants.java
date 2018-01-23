package com.shibu.flacify;

public class Constants
{
    public interface ACTION
    {
        public static String MAIN_ACTION="com.shibu.flacify.action.main";
        public static String PREV_ACTION="com.shibu.flacify.action.prev";
        public static String PLAY_ACTION="com.shibu.flacify.action.play";
        public static String NEXT_ACTION="com.shibu.flacify.action.next";
        public static String STOP_ACTION="com.shibu.flacify.action.stop";
        public static String STARTFOREGROUND_ACTION="com.shibu.flacify.action.startforeground";
        public static String STOPFOREGROUND_ACTION="com.shibu.flacify.action.stopforeground";
    }

    public interface NOTIFICATION_ID
    {
        public static int FOREGROUND_SERVICE=101;

    }
}
