import React from "react";

export default function DemoNotice({ children = "I contenuti contrassegnati come esempi non sono notizie, eventi, attività o offerte verificate." }) {
  return <div role="note" className="my-5 p-3 bg-amber-100 border-l-4 border-amber-600 text-amber-950 font-body text-sm">
    <strong>Barizone è in fase dimostrativa.</strong> {children}
  </div>;
}
