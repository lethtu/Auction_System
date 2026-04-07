import com.auction.server.model.AuctionSession;
import com.auction.server.model.AuctionStatus;
import com.auction.server.repository.AuctionSessionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/bidder")
public class BidderController {

    @Autowired
    private AuctionSessionRepository auctionSessionRepository;

    // API lấy danh sách đang đấu giá (có phân trang)
    @GetMapping("/active-sessions")
    public ResponseEntity<Page<AuctionSession>> getActiveSessions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        // Sắp xếp ưu tiên hiển thị những cái mới nhất (theo startTime giảm dần)
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "startTime"));

        // Gọi hàm phân trang từ Repository với trạng thái ACTIVE
        Page<AuctionSession> activeSessions = auctionSessionRepository.findByStatus(AuctionStatus.ACTIVE, pageable);

        return ResponseEntity.ok(activeSessions);
    }
}