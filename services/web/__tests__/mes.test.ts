import { describe, expect, test } from "vitest";
import { limitesDoMes, limitesUltimosMeses } from "@/lib/mes";

describe("limitesDoMes", () => {
  test("mês de 31 dias", () => {
    expect(limitesDoMes("2026-08")).toEqual({
      inicio: "2026-08-01",
      fim: "2026-08-31",
    });
  });

  test("mês de 30 dias", () => {
    expect(limitesDoMes("2026-04")).toEqual({
      inicio: "2026-04-01",
      fim: "2026-04-30",
    });
  });

  test("fevereiro em ano bissexto", () => {
    expect(limitesDoMes("2028-02")).toEqual({
      inicio: "2028-02-01",
      fim: "2028-02-29",
    });
  });

  test("fevereiro em ano não bissexto", () => {
    expect(limitesDoMes("2026-02")).toEqual({
      inicio: "2026-02-01",
      fim: "2026-02-28",
    });
  });
});

describe("limitesUltimosMeses", () => {
  test("3 meses dentro do mesmo ano", () => {
    expect(limitesUltimosMeses("2026-08", 3)).toEqual({
      inicio: "2026-06-01",
      fim: "2026-08-31",
    });
  });

  test("janela cruzando virada de ano", () => {
    expect(limitesUltimosMeses("2026-01", 3)).toEqual({
      inicio: "2025-11-01",
      fim: "2026-01-31",
    });
  });

  test("1 mês só = o próprio mês", () => {
    expect(limitesUltimosMeses("2026-08", 1)).toEqual({
      inicio: "2026-08-01",
      fim: "2026-08-31",
    });
  });
});
