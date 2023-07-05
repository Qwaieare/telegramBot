package skypro.telegrambot.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "notification_tasks")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class NotificationTask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(nullable = false)
    private String message;
    @Column(name = "chat_id", nullable = false)
    private long chatId;
    @Column(name = "notification_date_time", nullable = false)
    private LocalDateTime notificationDateTime;


}
