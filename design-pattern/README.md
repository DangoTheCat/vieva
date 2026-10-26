# Design Patterns — SWD392 Presentation Support

Thư mục này hỗ trợ bài thuyết trình *SWD392 - Design Patterns Presentation* (Canva, 31 slide).
Nội dung gồm: kiểm tra danh sách 23 pattern, góp ý cho deck, gợi ý pattern nên code,
các hướng làm, và một case study Java hoàn chỉnh cho **Strategy**.

```
design-pattern/
├── README.md                      ← file này: tổng quan, gợi ý, hướng làm
└── strategy/                      ← case study đã code xong (Java)
    ├── README.md                  ← giải thích code theo class diagram, Q&A
    ├── diagram/strategy-class-diagram.puml
    ├── run.ps1 / run.sh           ← compile + chạy
    └── src/com/swd392/strategy/
        ├── PaymentStrategy.java          (Strategy)
        ├── CashOnDeliveryStrategy.java   (ConcreteStrategy)
        ├── CreditCardStrategy.java       (ConcreteStrategy)
        ├── MoMoWalletStrategy.java       (ConcreteStrategy)
        ├── ZaloPayStrategy.java          (ConcreteStrategy, thêm sau để demo Open/Closed)
        ├── PaymentContext.java           (Context)
        ├── Main.java                     (Client)
        ├── Trace.java                    (chỉ in log, không thuộc pattern)
        └── before/CheckoutWithoutPattern.java  (code if/else "trước khi dùng pattern")
```

---

## 1. Có đúng là 23 pattern không?

**Đúng.** Đó là 23 pattern **GoF (Gang of Four)** trong sách
*Design Patterns: Elements of Reusable Object-Oriented Software* (1994) của Erich Gamma,
Richard Helm, Ralph Johnson và John Vlissides. Chia thành 3 nhóm:

| Nhóm | Số lượng | Pattern |
|---|---|---|
| **Creational** (khởi tạo object) | 5 | Factory Method, Abstract Factory, Builder, Prototype, Singleton |
| **Structural** (ghép class/object) | 7 | Adapter, Bridge, Composite, Decorator, Facade, Flyweight, Proxy |
| **Behavioral** (phân chia trách nhiệm, giao tiếp) | 11 | Chain of Responsibility, Command, Interpreter, Iterator, Mediator, Memento, Observer, State, Strategy, Template Method, Visitor |

5 + 7 + 11 = **23**. Slide "Classification of the 23 GoF Design Patterns" trong deck đã đúng.

Lưu ý khi bị hỏi: MVC, Repository, Dependency Injection, Null Object, Object Pool... cũng là
pattern, nhưng **không** thuộc 23 GoF.

---

## 2. Góp ý cho deck Canva (đã đọc toàn bộ nội dung)

| # | Vị trí | Vấn đề | Sửa thành |
|---|---|---|---|
| 1 | Header các slide pattern 06 → 23 | Tất cả đều ghi `CREATIONAL PATTERNS` | Pattern 06–12: `STRUCTURAL PATTERNS`. Pattern 13–23: `BEHAVIORAL PATTERNS` |
| 2 | Agenda mục 05 | `C# Case Study & Dynamic Execution Tracing` | `Java Case Study & Dynamic Execution Tracing` |
| 3 | Mục 05 | Agenda có nhắc, nhưng deck **chưa có slide** case study | Thêm 4–5 slide theo outline ở mục 5 bên dưới |
| 4 | Slide 8. Composite | Tiêu đề `Composite (Object Tree` thiếu `)` | `Composite (Object Tree)` |
| 5 | Slide 15. Interpreter | `Expression: ... (Interpreter(context))` | `interpret(context)` (tên method, không phải tên pattern) |

---

## 3. Nên chọn pattern nào để code?

| Pattern | Nhóm | Dễ hiểu | Diagram để giải thích | Demo terminal | Ghi chú |
|---|---|---|---|---|---|
| **Strategy** ⭐ | Behavioral | Rất dễ | 4 vai trò, vừa đủ | Rõ: mỗi strategy cho ra số tiền khác | Agenda mục 05 của deck **đã chọn sẵn** Strategy. Project Vieva cũng đang dùng (xem mục 4, hướng C) |
| Observer | Behavioral | Dễ | 4 vai trò | Rõ: 1 sự kiện, nhiều subscriber phản ứng | Ví dụ: công bố điểm thi, thông báo cho sinh viên, giảng viên, email |
| Factory Method | Creational | Trung bình | 4 vai trò, có kế thừa | Ít "wow" | Hay bị nhầm với Simple Factory và Abstract Factory |
| Decorator | Structural | Trung bình | Có đệ quy (wrapper bọc wrapper) | Vui: trà sữa + topping | Khó giải thích hơn vì Decorator vừa *là* vừa *có* Component |
| Singleton | Creational | Dễ nhất | Chỉ **1 class** | Nhàm | Ít nội dung để giải thích. Dễ bị hỏi khó về thread-safety, và nhiều người xem nó là anti-pattern |

**Khuyến nghị: Strategy.** Lý do:

1. Khớp sẵn với agenda mục 05 của deck ("Strategy Pattern case study, terminal execution, and 5-step diagram mapping"), nên không phải sửa cấu trúc bài.
2. Class diagram nhỏ (Context, Strategy, ConcreteStrategies, Client) và có đúng **5 điểm đánh số** để map với code.
3. Ví dụ thanh toán (COD / thẻ / MoMo / ZaloPay) ai cũng hiểu, không cần giải thích nghiệp vụ.
4. Thể hiện rõ các nguyên lý hay được hỏi: Open/Closed, composition over inheritance, programming to an interface.
5. Có ví dụ thật trong JDK (`Comparator`) và trong chính project Vieva (`FileStoragePort`).

---

## 4. Các hướng làm

Có thể kết hợp nhiều hướng. Code cho các hướng A, B, D đã có sẵn trong `strategy/`.

### Hướng A — Console demo Java thuần (✅ đã code)

Bài toán thanh toán đơn hàng 500.000 VND. Output terminal gắn tag vai trò và số trên diagram
(`[Client] #5`, `[Context] #1 #4`, `[ConcreteStrategy] #3`), nên chụp màn hình là
map được ngay với class diagram. Chi tiết xem [strategy/README.md](strategy/README.md).

### Hướng B — Before / After refactoring (✅ đã code)

Mở đầu bằng `before/CheckoutWithoutPattern.java` (chuỗi `if/else`), chỉ ra nhược điểm,
sau đó refactor thành Strategy. Đây là cách dễ thuyết phục nhất: người nghe thấy *vì sao* cần pattern.

### Hướng C — Liên hệ project thật Vieva

Backend Vieva dùng Strategy kết hợp Spring DI:
`FileStoragePort` (Strategy) có hai implementation, `LocalFileStorageGateway` và `CloudinaryStorageGateway`
(ConcreteStrategy). Spring chọn implementation bằng `vieva.storage.provider` (`@ConditionalOnProperty`).
`DocumentServiceImpl` đóng vai Context. Một slide này cho thấy nhóm hiểu pattern trong hệ thống thật,
không chỉ trong ví dụ sách vở.

### Hướng D — Live demo Open/Closed (✅ đã code)

`ZaloPayStrategy` là class "thêm sau". Khi demo, chỉ ra rằng `PaymentContext.java` không đổi một dòng nào.
Có thể code live: tạo class mới, `implements PaymentStrategy`, thêm 1 dòng `setStrategy(...)` trong `Main`, chạy lại.

### Hướng E — Bonus Java 8 lambda (cho phần Q&A)

`PaymentStrategy` chỉ có một method, nên là functional interface:
`context.setStrategy(amount -> amount * 90 / 100);`. Dùng để trả lời câu hỏi
"Java hiện đại còn cần Strategy không?": vẫn cần, chỉ là viết gọn hơn.

### Nếu nhóm muốn đổi pattern khác

- **Observer:** `ExamResultPublisher` + `StudentSubscriber`, `LecturerSubscriber`, `EmailSubscriber`.
- **Factory Method:** `QuestionCreator` → `MultipleChoiceCreator`, `EssayCreator`, `OralQuestionCreator`.
- **Decorator:** `Beverage` → `MilkTea`, `PearlTopping`, `CheeseFoamTopping` (tính giá cộng dồn).

Muốn code thêm pattern nào thì tạo folder ngang hàng với `strategy/`, giữ cùng cấu trúc.

---

## 5. Outline gợi ý cho mục 05 trong deck (Java Case Study)

| Slide | Nội dung | Lấy từ |
|---|---|---|
| 05.1 Problem | Code `if/else` + 3 nhược điểm | `strategy/README.md` mục 2 |
| 05.2 Class diagram | Diagram Strategy (5 số) đặt cạnh diagram Java | `strategy/README.md` mục 3, file `.puml` |
| 05.3 Role mapping | Bảng Context/Strategy/ConcreteStrategy/Client ↔ class Java | `strategy/README.md` mục 4 |
| 05.4 Code walkthrough | 5 đoạn code ngắn ứng với ①–⑤ | `strategy/README.md` mục 5 |
| 05.5 Terminal execution | Screenshot output, khoanh tag `#1`–`#5` | Chạy `run.ps1` |
| 05.6 Real-world (tuỳ chọn) | `Comparator` (JDK) + `FileStoragePort` (Vieva) | `strategy/README.md` mục 8, 9 |

---

## 6. Cách chạy

Yêu cầu: JDK 17 trở lên (đã chạy thử trên JDK 21).

```powershell
.\design-pattern\strategy\run.ps1
```

```bash
./design-pattern/strategy/run.sh
```

Hoặc chạy bằng tay:

```bash
javac -encoding UTF-8 -d design-pattern/strategy/out $(find design-pattern/strategy/src -name '*.java')
java -cp design-pattern/strategy/out com.swd392.strategy.Main
```

Mở bằng IntelliJ: *File → Open* thư mục `design-pattern/strategy`, đánh dấu `src` là *Sources Root*, rồi chạy `Main`.
