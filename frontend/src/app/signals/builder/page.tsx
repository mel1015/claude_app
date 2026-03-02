"use client";

import { useRouter } from "next/navigation";
import { SignalBuilder } from "@/components/signals/SignalBuilder";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/Card";
import { ArrowLeft, Bell } from "lucide-react";
import Link from "next/link";

export default function SignalBuilderPage() {
  const router = useRouter();

  return (
    <div className="space-y-6">
      <div className="flex items-center gap-4">
        <Link href="/signals" className="p-2 rounded-md hover:bg-accent">
          <ArrowLeft className="h-5 w-5" />
        </Link>
        <h1 className="flex items-center gap-2 text-2xl font-bold">
          <Bell className="h-6 w-6" />
          새 시그널 만들기
        </h1>
      </div>

      <Card>
        <CardHeader>
          <CardTitle className="text-base">시그널 조건 설정</CardTitle>
        </CardHeader>
        <CardContent>
          <SignalBuilder onSaved={() => router.push("/signals")} />
        </CardContent>
      </Card>
    </div>
  );
}
