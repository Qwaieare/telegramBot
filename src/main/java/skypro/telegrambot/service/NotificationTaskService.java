package skypro.telegrambot.service;

import org.springframework.stereotype.Service;
import skypro.telegrambot.entity.NotificationTask;
import skypro.telegrambot.repository.NotificationTaskRepository;

@Service
public class NotificationTaskService {
    private final NotificationTaskRepository notificationTaskRepository;

    public NotificationTaskService(NotificationTaskRepository notificationTaskRepository) {
        this.notificationTaskRepository = notificationTaskRepository;
    }

    public void save(NotificationTask notificationTask) {
        notificationTaskRepository.save(notificationTask);
    }
}
