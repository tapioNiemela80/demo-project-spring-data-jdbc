package tn.demo.common;

import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Component
public class DateService {
    public LocalDate today(){
        return LocalDate.now();
    }
    public LocalDateTime now(){
        return LocalDateTime.now();
    }
}
