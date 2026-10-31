# Arch check (31/10/2026)

- `ArchitectureTest.java`: 14 luật gốc từ commit ed43893 + 3 luật đề xuất (X1 gateway AI, X2 infrastructure→usecases, X3 entity không có public setter).
  Dùng: copy vào `BE/src/test/java/com/example/vieva/architecture/`, thêm dependency
  `com.tngtech.archunit:archunit-junit5:1.3.0` (scope test) vào `pom.xml`, chạy `mvn test -Dtest=ArchitectureTest`.
  Lưu ý: X1 áp cho các output port tên *Gateway, *GraderPort và AttemptGradingResult (repository dùng entity là hợp lệ)
- `archscan.py`: quét import tĩnh tương đương các luật trên (không cần Maven). Chạy từ `BE/src/main/java`: `python3 ../../../docs/arch-check/archscan.py`.
- `archscan_output.txt`: kết quả ngày 31/10/2026.
