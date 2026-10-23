import cn.iocoder.yudao.module.dcc.service.file.DccApplicationEffectiveSelection;
import java.time.LocalDate;
public class SelectionProbe {
  public static void main(String[] args) {
    if (!DccApplicationEffectiveSelection.isComplete("DELAYED", null))
      throw new AssertionError("DELAYED/null submission is currently rejected by real production selection policy");
    if (DccApplicationEffectiveSelection.isComplete("DELAYED", LocalDate.of(2026,10,15)))
      throw new AssertionError("DELAYED must not accept a submission date");
    DccApplicationEffectiveSelection.requireSubmission("DELAYED", null, LocalDate.of(2026,10,15));
    System.out.println("G01 production selection policy PASS");
  }
}
