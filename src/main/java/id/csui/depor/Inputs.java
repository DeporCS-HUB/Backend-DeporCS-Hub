package id.csui.depor;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import java.util.List;
public class Inputs {
 public record Profile(@NotBlank @Size(max=120) String name) {}
 public record Event(@NotBlank @Size(max=160) String name,@Size(max=2000) String description,UUID program_id,@NotBlank @Size(max=160) String venue,@NotNull LocalDate start_date,@NotNull LocalDate end_date,@NotBlank @Pattern(regexp="Planning|Confirmed|Completed|Cancelled") String status,@NotBlank @Pattern(regexp="Not required|Pending|Approved|Rejected") String permit_status) {
  @com.fasterxml.jackson.annotation.JsonIgnore @AssertTrue public boolean isDateRangeValid() {return start_date==null||end_date==null||!end_date.isBefore(start_date);}
 }

 public record Program(@NotBlank @Size(max=160) String name,@Size(max=2000) String description,@NotBlank @Size(max=120) String pic,UUID pic_id,LocalDate start_date,LocalDate end_date,@NotBlank @Pattern(regexp="Planning|Ongoing|Active|Completed|Cancelled|Unspecified") String status,@Min(0) @Max(100) Integer progress,@DecimalMin("0") @Digits(integer=13,fraction=2) BigDecimal budget,@Pattern(regexp="Proker|UKOR") String kind,@Size(max=2000) String progress_notes,@Size(max=160) String next_milestone,LocalDate milestone_date,@Size(max=30) List<@NotNull UUID> assignee_ids) {
  @com.fasterxml.jackson.annotation.JsonIgnore @AssertTrue public boolean isDateRangeValid() {return start_date==null||end_date==null||!end_date.isBefore(start_date);}
 }
 public record ProgramProgress(@NotBlank @Pattern(regexp="Planning|Ongoing|Active|Completed|Cancelled|Unspecified") String status,@Min(0) @Max(100) Integer progress,@Size(max=2000) String progress_notes) {}
 public record Task(@NotBlank @Size(max=160) String title,@Size(max=2000) String description,UUID program_id,UUID assignee_id,@NotBlank @Pattern(regexp="Backlog|To Do|In Progress|Review|Done") String status,@NotBlank @Pattern(regexp="Low|Medium|High") String priority,LocalDate due_date) {}
 public record Finance(@NotBlank @Size(max=200) String description,UUID program_id,@NotBlank @Pattern(regexp="income|expense") String type,@NotNull @DecimalMin(value="0",inclusive=false) @Digits(integer=13,fraction=2) BigDecimal amount,@NotBlank @Size(max=80) String category,@NotNull LocalDate transaction_date,@NotBlank @Pattern(regexp="pending|approved|rejected") String status) {}
 public record Inventory(@NotBlank @Size(max=160) String name,@NotBlank @Size(max=80) String category,@NotNull @Min(0) @Max(1000000) Integer quantity,@NotBlank @Pattern(regexp="Available|Borrowed|Maintenance|Lost") String status,@NotBlank @Pattern(regexp="Baik|Perawatan|Rusak") String condition,@Size(max=16) String emoji,@Size(max=160) String location) {}
}
