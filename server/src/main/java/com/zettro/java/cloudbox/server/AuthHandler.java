package com.zettro.java.cloudbox.server;

import com.zettro.java.cloudbox.common.AuthAnswer;
import com.zettro.java.cloudbox.common.AuthRequest;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.apache.log4j.Logger;

import java.nio.file.Files;
import java.nio.file.Paths;

public class AuthHandler extends ChannelInboundHandlerAdapter {

    Logger stdLogger = CloudBoxServer.stdLogger;
    String serverStoragePath = CloudBoxServer.serverStoragePath;
    String textMessage;

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        stdLogger.info("Клиент подключился.");
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        stdLogger.info("Клиент отключился.");
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof AuthRequest) {
            AuthRequest authRequest = (AuthRequest) msg;
            String username = authRequest.getUsername();
            String password = authRequest.getPassword();
            boolean newUser = authRequest.isNewUser();

            if (username.equals("") || password.equals("")) {
                textMessage = "Неудачная аутентификация. Имя и пароль не могут быть пустыми.";
                stdLogger.info(msg);
                ctx.writeAndFlush(new AuthAnswer(AuthAnswer.AuthResult.FAILED, textMessage));
                return;
            }
            // сначала проверяем по базе
            if (newUser) {
                // создаем нового пользователя
                try {
                    if (Files.exists(Paths.get(serverStoragePath, username))) {
                        throw new Exception("Ошибка регистрации. Пользователь с таким именем уже зарегистрирован!");
                    }
                    SqlClient.createUser(username, password);
                    Files.createDirectory(Paths.get(serverStoragePath, username));
                } catch (Exception e) {
                    e.printStackTrace();
                    stdLogger.info(e.getMessage());
                    ctx.writeAndFlush(new AuthAnswer(AuthAnswer.AuthResult.FAILED, e.getMessage()));
                    return;
                }
            } else if (!SqlClient.isRegisteredUser(username, password)) {
                textMessage = "Неудачная аутентификация. Указаны неверные учетные данные.";
                stdLogger.info(textMessage);
                ctx.writeAndFlush(new AuthAnswer(AuthAnswer.AuthResult.FAILED, textMessage));
                return;
            }

            // проверяем наличие локального каталога
            String userPath = serverStoragePath + "\\" + username;
            if (!Files.exists(Paths.get(userPath)) || (!Files.isDirectory(Paths.get(userPath)))) {
                textMessage = "Произошла ошибка на сервере. Пользовательский каталог не существует.\n" +
                        "Обратитесь в техничекую поддержку по тел. 123-456-789.";
                stdLogger.info("Произошла ошибка на сервере. Пользовательский каталог не существует");
                ctx.writeAndFlush(new AuthAnswer(AuthAnswer.AuthResult.FAILED, textMessage));
                return;
            }
            textMessage = "Облачное хранилище CloudBox к Вашим услугам!";
            stdLogger.info("Успешная аутентификация!");
            ctx.writeAndFlush(new AuthAnswer(AuthAnswer.AuthResult.PASSED, textMessage));
            // самоудаляемся и добавляем нормальный обработчик входного потока
            ctx.pipeline().addLast(new MainHandler(username));
            ctx.pipeline().remove(this);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }
}
