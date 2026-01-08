package br.com.prospectaai.shared.kafka;

public enum KafkaTopic {
    USER_CREATED("user.created"),
    USER_UPDATED("user.updated"),
    USER_DELETED("user.deleted"),
    EMAIL_SENDER("email.sender"),
    N8N_ASYNC_TASK("async.task.n8n"),
    N8N_ASYNC_TASK_REQUEST("async.task.n8n.request"),
    N8N_ASYNC_TASK_RESPONSE("async.task.n8n.response"),
    N8N_ASYNC_TASK_ERROR("async.task.n8n.error"),
    NOTIFICATION("notification"),
    NOTIFICATION_PROSPECTION("notification.prospection");

    private final String topic;

    KafkaTopic(String topic) {
        this.topic = topic;
    }

    public String getTopic() {
        return topic;
    }

    @Override
    public String toString() {
        return topic;
    }
}
