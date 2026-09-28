import * as React from "react";
import * as ScrollAreaPrimitive from "@radix-ui/react-scroll-area";

export function ScrollArea({ className = "", children, ...props }) {
  return (
    <ScrollAreaPrimitive.Root
      className={`relative overflow-hidden ${className}`}
      {...props}
    >
      <ScrollAreaPrimitive.Viewport className="h-full w-full rounded-[inherit]">
        {children}
      </ScrollAreaPrimitive.Viewport>
      <ScrollBar />
      <ScrollAreaPrimitive.Corner />
    </ScrollAreaPrimitive.Root>
  );
}

export function ScrollBar({ className = "", orientation = "vertical", ...props }) {
  return (
    <ScrollAreaPrimitive.ScrollAreaScrollbar
      orientation={orientation}
      className={`flex touch-none select-none transition-colors duration-150 ease-out ${
        orientation === "vertical"
          ? "h-full w-2.5 border-l border-l-transparent p-[1px] hover:w-3"
          : "h-2.5 flex-col border-t border-t-transparent p-[1px] hover:h-3"
      } ${className}`}
      {...props}
    >
      <ScrollAreaPrimitive.ScrollAreaThumb className="relative flex-1 rounded-full bg-slate-300/80 hover:bg-slate-400 dark:bg-slate-700 transition-colors" />
    </ScrollAreaPrimitive.ScrollAreaScrollbar>
  );
}

export default ScrollArea;
