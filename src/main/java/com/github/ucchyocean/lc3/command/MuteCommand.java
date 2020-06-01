/*
 * @author     ucchy
 * @license    LGPLv3
 * @copyright  Copyright ucchy 2020
 */
package com.github.ucchyocean.lc3.command;

import com.github.ucchyocean.lc3.channel.Channel;
import com.github.ucchyocean.lc3.member.ChannelMember;

/**
 * muteコマンドの実行クラス
 * @author ucchy
 */
public class MuteCommand extends SubCommandAbst {

    private static final String COMMAND_NAME = "mute";
    private static final String PERMISSION_NODE = "lunachat." + COMMAND_NAME;
    private static final String USAGE_KEY1 = "usageMute";
    private static final String USAGE_KEY2 = "usageMute2";

    /**
     * コマンドを取得します。
     * @return コマンド
     * @see com.github.ucchyocean.lc3.command.SubCommandAbst#getCommandName()
     */
    @Override
    public String getCommandName() {
        return COMMAND_NAME;
    }

    /**
     * パーミッションノードを取得します。
     * @return パーミッションノード
     * @see com.github.ucchyocean.lc3.command.SubCommandAbst#getPermissionNode()
     */
    @Override
    public String getPermissionNode() {
        return PERMISSION_NODE;
    }

    /**
     * コマンドの種別を取得します。
     * @return コマンド種別
     * @see com.github.ucchyocean.lc3.command.SubCommandAbst#getCommandType()
     */
    @Override
    public CommandType getCommandType() {
        return CommandType.MODERATOR;
    }

    /**
     * 使用方法に関するメッセージをsenderに送信します。
     * @param sender コマンド実行者
     * @param label 実行ラベル
     * @see com.github.ucchyocean.lc3.command.SubCommandAbst#sendUsageMessage()
     */
    @Override
    public void sendUsageMessage(
            ChannelMember sender, String label) {
        sendResourceMessage(sender, "", USAGE_KEY1, label);
        sendResourceMessage(sender, "", USAGE_KEY2, label);
    }

    /**
     * コマンドを実行します。
     * @param sender コマンド実行者
     * @param label 実行ラベル
     * @param args 実行時の引数
     * @return コマンドが実行されたかどうか
     * @see com.github.ucchyocean.lc3.command.SubCommandAbst#runCommand(java.lang.String[])
     */
    @Override
    public boolean runCommand(
            ChannelMember sender, String label, String[] args) {

        // 実行引数から、Muteするユーザーを取得する
        String kickedName = "";
        if (args.length >= 2) {
            kickedName = args[1];
        } else {
            sendResourceMessage(sender, PREERR, "errmsgCommand");
            return true;
        }

        // 対象チャンネルを取得、取得できない場合はエラー表示して終了する
        boolean isSpecifiedChannel = false;
        Channel channel = null;
        if (args.length >= 3) {
            channel = api.getChannel(args[2]);
            isSpecifiedChannel = true;
        } else {
            channel = api.getDefaultChannel(sender.getName());
        }
        if (channel == null) {
            sendResourceMessage(sender, PREERR, "errmsgNoJoin");
            return true;
        }

        // モデレーターかどうか確認する
        if ( !channel.hasModeratorPermission(sender) ) {
            sendResourceMessage(sender, PREERR, "errmsgNotModerator");
            return true;
        }

        // Muteされるプレイヤーがメンバーかどうかチェックする
        ChannelMember kicked = ChannelMember.getChannelMember(kickedName);
        if (!channel.getMembers().contains(kicked)) {
            sendResourceMessage(sender, PREERR, "errmsgNomemberOther");
            return true;
        }

        // 既にMuteされているかどうかチェックする
        if (channel.getMuted().contains(kicked)) {
            sendResourceMessage(sender, PREERR, "errmsgAlreadyMuted");
            return true;
        }

        // 期限付きMuteの場合、期限の指定が正しいかどうかをチェックする
        int expireMinutes = -1;
        if (args.length >= 3 && !isSpecifiedChannel) {
            if ( !args[2].matches("[0-9]+") ) {
                sendResourceMessage(sender, PREERR, "errmsgInvalidMuteExpireParameter");
                return true;
            }
            expireMinutes = Integer.parseInt(args[2]);
            if ( expireMinutes < 1 || 43200 < expireMinutes ) {
                sendResourceMessage(sender, PREERR, "errmsgInvalidMuteExpireParameter");
                return true;
            }
        }

        // Mute実行
        channel.getMuted().add(kicked);
        if ( expireMinutes != -1 ) {
            long expire = System.currentTimeMillis() + expireMinutes * 60 * 1000;
            channel.getMuteExpires().put(kicked, expire);
        }
        channel.save();

        // senderに通知メッセージを出す
        if ( expireMinutes != -1 ) {
            sendResourceMessage(sender, PREINFO,
                    "cmdmsgMuteWithExpire", kickedName, channel.getName(), expireMinutes);
        } else {
            sendResourceMessage(sender, PREINFO,
                    "cmdmsgMute", kickedName, channel.getName());
        }

        // チャンネルに通知メッセージを出す
        if ( expireMinutes != -1 ) {
            sendResourceMessageWithKeyword(channel,
                    "muteWithExpireMessage", kicked, expireMinutes);
        } else {
            sendResourceMessageWithKeyword(channel, "muteMessage", kicked);
        }

        // BANされた人に通知メッセージを出す
        if ( kicked != null ) {
            sendResourceMessage(kicked, PREINFO,
                    "cmdmsgMuted", channel.getName());
        }

        return true;
    }
}