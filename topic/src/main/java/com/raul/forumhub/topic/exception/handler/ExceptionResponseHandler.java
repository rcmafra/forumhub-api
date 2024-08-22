package com.raul.forumhub.topic.exception.handler;

import com.raul.forumhub.topic.exception.AbstractServiceException;
import com.raul.forumhub.topic.exception.AnswerServiceException;
import com.raul.forumhub.topic.exception.InstanceNotFoundException;
import com.raul.forumhub.topic.exception.TopicServiceException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.concurrent.TimeoutException;

@RestControllerAdvice
public class ExceptionResponseHandler {

    public HttpHeaders headers(){
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PROBLEM_JSON);
        headers.setContentLanguage(new Locale("Pt", "BR"));
        return headers;
    }

    @ExceptionHandler(ConstraintViolationException.class)
    private ResponseEntity<ExceptionEntity> constraintExceptionResolver(ConstraintViolationException ex, HttpServletRequest request){
        String detail = ex.getConstraintViolations().stream().map(ConstraintViolation::getMessageTemplate)
                .findFirst().orElse("Erro de violação de restrição");
        ExceptionEntity entity = new ExceptionEntity(LocalDateTime.now(), HttpStatus.BAD_REQUEST.value(),
                "Erro de restrição", detail, request.getRequestURI());
        return new ResponseEntity<>(entity, headers(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    private ResponseEntity<ExceptionEntity> duplicatedDataExceptionResolver(DataIntegrityViolationException ex, HttpServletRequest request){
        ExceptionEntity entity = new ExceptionEntity(LocalDateTime.now(), HttpStatus.CONFLICT.value(),
                "Erro de restrição", "Registro já existente", request.getRequestURI());
        return new ResponseEntity<>(entity, headers(), HttpStatus.CONFLICT);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    private ResponseEntity<ExceptionEntity> notReadableExceptionResolver(HttpMessageNotReadableException ex, HttpServletRequest request){
        ExceptionEntity entity = new ExceptionEntity(LocalDateTime.now(), HttpStatus.BAD_REQUEST.value(),
                "Solicitação desconhecida", "Solicitação com valor ilegível", request.getRequestURI());
        return new ResponseEntity<>(entity, headers(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(WebClientResponseException.class)
    public ResponseEntity<ExceptionEntity> webClientExceptionResolver(WebClientResponseException ex, HttpServletRequest request){
        ExceptionEntity entity = new ExceptionEntity(LocalDateTime.now(), HttpStatus.SERVICE_UNAVAILABLE.value(),
                "Serviço indisponível", "O serviço solicitado está fora do ar", request.getRequestURI());
        return new ResponseEntity<>(entity, headers(), HttpStatus.SERVICE_UNAVAILABLE);
    }

    @ExceptionHandler(TimeoutException.class)
    public ResponseEntity<ExceptionEntity> clientTimeoutExceptionResolver(TimeoutException ex, HttpServletRequest request){
        ExceptionEntity entity = new ExceptionEntity(LocalDateTime.now(), HttpStatus.SERVICE_UNAVAILABLE.value(),
                "Serviço indisponível", "O serviço solicitado está fora do ar", request.getRequestURI());
        return new ResponseEntity<>(entity, headers(), HttpStatus.SERVICE_UNAVAILABLE);
    }

    @ExceptionHandler({AbstractServiceException.class, TopicServiceException.class, AnswerServiceException.class})
    public ResponseEntity<ExceptionEntity> businessServiceExceptionResolver(AbstractServiceException ex, HttpServletRequest request){
        ExceptionEntity entity = new ExceptionEntity(LocalDateTime.now(), HttpStatus.BAD_REQUEST.value(),
                "Erro de negócio", ex.getMessage(), request.getRequestURI());
        return new ResponseEntity<>(entity, headers(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(InstanceNotFoundException.class)
    public ResponseEntity<ExceptionEntity> instanceNotFoundExceptionResolver(InstanceNotFoundException ex, HttpServletRequest request){
        ExceptionEntity entity = new ExceptionEntity(LocalDateTime.now(), HttpStatus.NOT_FOUND.value(),
                "Solicitação não encontrada", ex.getMessage(), request.getRequestURI());
        return new ResponseEntity<>(entity, headers(), HttpStatus.NOT_FOUND);
    }

}
