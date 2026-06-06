package made.archive.entite;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import jakarta.persistence.Table;

import java.time.LocalDateTime;


@Entity
@Table(name="retentions")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Retention 
{
    @Id
    @GeneratedValue    
    private Long id;

    @NotNull
    @Column(nullable = false)
    private Long retentionYears;

    @NotNull
    @Column(nullable = false)
    private Long periodGrace;

    @NotNull
    @Column(nullable = false)
    private LocalDateTime createAt = LocalDateTime.now();
    
}
