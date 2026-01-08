package br.com.prospectaai.shared.util;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public class DatePeriodFormatter {
    public static String format(Instant from, Instant to) {
        if (from == null || to == null) return "";
        Duration d = Duration.between(from, to).abs();
        long seconds = d.getSeconds();
        if (seconds < 45) return "Agora";
        long minutes = seconds / 60;
        if (minutes < 2) return "1 minuto";
        if (minutes < 60) return minutes + " minutos";
        long hours = minutes / 60;
        if (hours < 2) return "1 hora";
        if (hours < 24) return hours + " horas";
        long days = hours / 24;
        if (days < 2) return "1 dia";
        if (days < 7) return days + " dias";
        long weeks = days / 7;
        if (weeks < 2) return "1 semana";
        if (weeks < 4) return weeks + " semanas";

        ZoneId zone = ZoneId.systemDefault();
        LocalDate start = LocalDate.ofInstant(from, zone);
        LocalDate end = LocalDate.ofInstant(to, zone);
        if (start.isAfter(end)) {
            LocalDate tmp = start; start = end; end = tmp;
        }
        long years = ChronoUnit.YEARS.between(start, end);
        if (years == 0) {
            long months = ChronoUnit.MONTHS.between(start, end);
            if (months < 2) return "1 mês";
            return months + " meses";
        }
        if (years < 2) return "1 ano";
        if (years < 10) return years + " anos";
        long decades = years / 10;
        if (decades < 2) return "1 década";
        if (decades < 10) return decades + " décadas";
        long centuries = years / 100;
        if (centuries < 2) return "1 século";
        return centuries + " séculos";
    }
}
