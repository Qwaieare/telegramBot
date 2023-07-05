package skypro.telegrambot.listener;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.PhotoSize;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.GetFile;
import com.pengrad.telegrambot.request.SendMessage;
import com.pengrad.telegrambot.response.GetFileResponse;
import com.pengrad.telegrambot.response.SendResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import skypro.telegrambot.entity.NotificationTask;
import skypro.telegrambot.service.NotificationTaskService;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.time.LocalDateTime.parse;

@Component
public
class TelegramBotUpdatesListener implements UpdatesListener {
    private final TelegramBot telegramBot;
    private final Logger logger = LoggerFactory.getLogger(TelegramBotUpdatesListener.class);
    // регулярное выражение
    private final Pattern pattern = Pattern.compile("(\\d{1,2}\\.\\d{1,2}\\.\\d{4} \\d{1,2}:\\d{2})\\s+([А-я\\d\\s.,!?:]+)");
    private final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
    private final NotificationTaskService notificationTaskService;


    public TelegramBotUpdatesListener(TelegramBot telegramBot, NotificationTaskService notificationTaskService) {
        this.telegramBot = telegramBot;
        this.notificationTaskService = notificationTaskService;
    }



    // связываем Телеграмм бот c Listener
    @PostConstruct
    public void init() {
        telegramBot.setUpdatesListener(this);
    }


    @Override
    public int process(List<Update> updates) {
        try {
            updates.stream()
                    .filter(update -> update.message() != null)
                    .forEach(update -> {
              logger.info("Handles update: {}", update);
              Message message = update.message();
              Long chatId = message.chat().id();
              String text = message.text();

              if ("/start".equals(text)) {
                  SendMessage sendMessage =new SendMessage(chatId, "Привет! Я помогу вам запланировать задачу. " +
                          "Пожалуйста отправьте её мне в следующем формате: 09.06.2023 15:00");
              } else if (text != null) {
                  Matcher matcher = pattern.matcher(text);
                  if (matcher.find()) {
                      LocalDateTime dateTime = parse(matcher.group(1));

                      if (Objects.isNull(dateTime)) {
                          sendMessage(chatId, "Некорректный формат даты и/или времени!");
                      } else {
                          String txt = matcher.group(2);
                          NotificationTask notificationTask = new NotificationTask();
                          notificationTask.setChatId(chatId);
                          notificationTask.setMessage(txt);
                          notificationTask.setNotificationDateTime(dateTime);
                          notificationTaskService.save(notificationTask);
                          sendMessage(chatId, "Задача успешно запланирована!");
                      }
                      } else {
                      sendMessage(chatId, "Некорректный формат сообщения!");
                  }
              } else if (message.photo() != null) {
                  PhotoSize photoSize = message.photo()[message.photo().length - 1];
                  GetFileResponse getFileResponse = telegramBot.execute(new GetFile(photoSize.fileId()));
                  if (getFileResponse.isOk()) {
                      try {
                          String extension = StringUtils.getFilenameExtension(getFileResponse.file().filePath());
                          byte[] image = telegramBot.getFileContent(getFileResponse.file());
                          Files.write(Paths.get(UUID.randomUUID() + "." + extension), image);
                      } catch (IOException e) {
                          throw new RuntimeException(e);
                      }
                  }
              }
                    });
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        return UpdatesListener.CONFIRMED_UPDATES_ALL;
    }


    // дополнительный метод по формату парсинга
    @Nullable
    private LocalDateTime parse(String dateTime) {
        try {
            return LocalDateTime.parse(dateTime, dateTimeFormatter);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private void sendMessage(Long chatId, String message) {
        SendMessage sendMessage = new SendMessage(chatId, message);
        SendResponse sendResponse = telegramBot.execute(sendMessage);
        if (!sendResponse.isOk()) {
            logger.error("Error during sending message: {}", sendResponse.description());
        }
    }


}
