"use client";

import { useState } from "react";
import { Wallet, ArrowDownToLine, Calendar, Loader2, Building2, CreditCard } from "lucide-react";
import { format } from "date-fns";
import { Header } from "@/components/layout/header";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from "@/components/ui/dialog";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { formatCurrency, formatDate } from "@/lib/utils";
import { useSettlements } from "@/lib/hooks/useSettlements";
import type { SettlementStatus } from "@/types";

const statusColors: Record<SettlementStatus, string> = {
  PENDING: "bg-yellow-100 text-yellow-800",
  PROCESSING: "bg-blue-100 text-blue-800",
  COMPLETED: "bg-green-100 text-green-800",
  FAILED: "bg-red-100 text-red-800",
  ON_HOLD: "bg-gray-100 text-gray-800",
};

const statusFilters: { label: string; value: SettlementStatus | "ALL" }[] = [
  { label: "All", value: "ALL" },
  { label: "Pending", value: "PENDING" },
  { label: "Processing", value: "PROCESSING" },
  { label: "Completed", value: "COMPLETED" },
  { label: "Failed", value: "FAILED" },
];

export default function SettlementsPage() {
  const {
    statusFilter,
    startDate,
    endDate,
    settlements,
    walletBalance,
    bankAccounts,
    isLoading,
    isLoadingWallet,
    setStatusFilter,
    setDateRange,
    requestWithdrawal,
    isWithdrawing,
  } = useSettlements();

  const [isWithdrawDialogOpen, setIsWithdrawDialogOpen] = useState(false);
  const [withdrawAmount, setWithdrawAmount] = useState("");
  const [selectedBankAccount, setSelectedBankAccount] = useState("");

  const handleWithdraw = () => {
    if (!withdrawAmount || !selectedBankAccount) return;

    const amount = parseFloat(withdrawAmount);
    if (isNaN(amount) || amount <= 0) return;

    requestWithdrawal({
      amount,
      bankAccountId: selectedBankAccount,
    });

    setIsWithdrawDialogOpen(false);
    setWithdrawAmount("");
    setSelectedBankAccount("");
  };

  const handleStartDateChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const newDate = e.target.value ? new Date(e.target.value) : undefined;
    setDateRange(newDate, endDate);
  };

  const handleEndDateChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const newDate = e.target.value ? new Date(e.target.value) : undefined;
    setDateRange(startDate, newDate);
  };

  return (
    <div>
      <Header title="Settlements & Wallet" />

      <div className="p-6 space-y-6">
        {/* Wallet Balance Cards */}
        <div className="grid gap-4 md:grid-cols-4">
          <Card>
            <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
              <CardTitle className="text-sm font-medium">Available Balance</CardTitle>
              <Wallet className="h-4 w-4 text-muted-foreground" />
            </CardHeader>
            <CardContent>
              {isLoadingWallet ? (
                <Loader2 className="h-4 w-4 animate-spin" />
              ) : (
                <>
                  <div className="text-2xl font-bold text-green-600">
                    {formatCurrency(walletBalance?.availableBalance ?? 0)}
                  </div>
                  <p className="text-xs text-muted-foreground">Ready to withdraw</p>
                </>
              )}
            </CardContent>
          </Card>

          <Card>
            <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
              <CardTitle className="text-sm font-medium">Pending Balance</CardTitle>
              <Calendar className="h-4 w-4 text-muted-foreground" />
            </CardHeader>
            <CardContent>
              {isLoadingWallet ? (
                <Loader2 className="h-4 w-4 animate-spin" />
              ) : (
                <>
                  <div className="text-2xl font-bold text-yellow-600">
                    {formatCurrency(walletBalance?.pendingBalance ?? 0)}
                  </div>
                  <p className="text-xs text-muted-foreground">Being processed</p>
                </>
              )}
            </CardContent>
          </Card>

          <Card>
            <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
              <CardTitle className="text-sm font-medium">Total Earnings</CardTitle>
              <CreditCard className="h-4 w-4 text-muted-foreground" />
            </CardHeader>
            <CardContent>
              {isLoadingWallet ? (
                <Loader2 className="h-4 w-4 animate-spin" />
              ) : (
                <>
                  <div className="text-2xl font-bold">
                    {formatCurrency(walletBalance?.totalEarnings ?? 0)}
                  </div>
                  <p className="text-xs text-muted-foreground">All time</p>
                </>
              )}
            </CardContent>
          </Card>

          <Card>
            <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
              <CardTitle className="text-sm font-medium">Total Withdrawn</CardTitle>
              <ArrowDownToLine className="h-4 w-4 text-muted-foreground" />
            </CardHeader>
            <CardContent>
              {isLoadingWallet ? (
                <Loader2 className="h-4 w-4 animate-spin" />
              ) : (
                <>
                  <div className="text-2xl font-bold">
                    {formatCurrency(walletBalance?.totalWithdrawn ?? 0)}
                  </div>
                  <p className="text-xs text-muted-foreground">All time</p>
                </>
              )}
            </CardContent>
          </Card>
        </div>

        {/* Withdrawal Button */}
        <div className="flex justify-end">
          <Dialog open={isWithdrawDialogOpen} onOpenChange={setIsWithdrawDialogOpen}>
            <DialogTrigger asChild>
              <Button disabled={(walletBalance?.availableBalance ?? 0) <= 0}>
                <ArrowDownToLine className="mr-2 h-4 w-4" />
                Request Withdrawal
              </Button>
            </DialogTrigger>
            <DialogContent>
              <DialogHeader>
                <DialogTitle>Request Withdrawal</DialogTitle>
                <DialogDescription>
                  Enter the amount you want to withdraw. Available balance:{" "}
                  {formatCurrency(walletBalance?.availableBalance ?? 0)}
                </DialogDescription>
              </DialogHeader>
              <div className="space-y-4 py-4">
                <div className="space-y-2">
                  <Label htmlFor="amount">Amount</Label>
                  <Input
                    id="amount"
                    type="number"
                    placeholder="Enter amount"
                    value={withdrawAmount}
                    onChange={(e) => setWithdrawAmount(e.target.value)}
                    max={walletBalance?.availableBalance ?? 0}
                  />
                </div>
                <div className="space-y-2">
                  <Label htmlFor="bank-account">Bank Account</Label>
                  <Select value={selectedBankAccount} onValueChange={setSelectedBankAccount}>
                    <SelectTrigger>
                      <SelectValue placeholder="Select bank account" />
                    </SelectTrigger>
                    <SelectContent>
                      {bankAccounts.map((account) => (
                        <SelectItem key={account.id} value={account.id}>
                          <div className="flex items-center gap-2">
                            <Building2 className="h-4 w-4" />
                            <span>{account.bankName}</span>
                            <span className="text-muted-foreground">
                              ****{account.accountNumberLast4}
                            </span>
                            {account.isPrimary && (
                              <span className="text-xs bg-primary/10 text-primary px-1 rounded">
                                Primary
                              </span>
                            )}
                          </div>
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                </div>
              </div>
              <DialogFooter>
                <Button variant="outline" onClick={() => setIsWithdrawDialogOpen(false)}>
                  Cancel
                </Button>
                <Button
                  onClick={handleWithdraw}
                  disabled={
                    !withdrawAmount ||
                    !selectedBankAccount ||
                    parseFloat(withdrawAmount) <= 0 ||
                    parseFloat(withdrawAmount) > (walletBalance?.availableBalance ?? 0) ||
                    isWithdrawing
                  }
                >
                  {isWithdrawing ? (
                    <>
                      <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                      Processing...
                    </>
                  ) : (
                    "Confirm Withdrawal"
                  )}
                </Button>
              </DialogFooter>
            </DialogContent>
          </Dialog>
        </div>

        {/* Filters */}
        <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
          <div className="flex flex-wrap gap-2">
            {statusFilters.map((filter) => (
              <Button
                key={filter.value}
                variant={statusFilter === filter.value ? "default" : "outline"}
                size="sm"
                onClick={() => setStatusFilter(filter.value)}
              >
                {filter.label}
              </Button>
            ))}
          </div>
          <div className="flex gap-2 items-center">
            <div className="flex items-center gap-2">
              <Label htmlFor="start-date" className="text-sm whitespace-nowrap">
                From:
              </Label>
              <Input
                id="start-date"
                type="date"
                className="w-auto"
                value={startDate ? format(startDate, "yyyy-MM-dd") : ""}
                onChange={handleStartDateChange}
              />
            </div>
            <div className="flex items-center gap-2">
              <Label htmlFor="end-date" className="text-sm whitespace-nowrap">
                To:
              </Label>
              <Input
                id="end-date"
                type="date"
                className="w-auto"
                value={endDate ? format(endDate, "yyyy-MM-dd") : ""}
                onChange={handleEndDateChange}
              />
            </div>
          </div>
        </div>

        {/* Loading state */}
        {isLoading && (
          <div className="flex items-center justify-center py-12">
            <Loader2 className="h-8 w-8 animate-spin text-muted-foreground" />
          </div>
        )}

        {/* Settlement History Table */}
        {!isLoading && (
          <Card>
            <CardContent className="p-0">
              <div className="overflow-x-auto">
                <table className="w-full">
                  <thead>
                    <tr className="border-b bg-muted/50">
                      <th className="px-4 py-3 text-left text-sm font-medium text-muted-foreground">
                        ID
                      </th>
                      <th className="px-4 py-3 text-left text-sm font-medium text-muted-foreground">
                        Amount
                      </th>
                      <th className="px-4 py-3 text-left text-sm font-medium text-muted-foreground">
                        Status
                      </th>
                      <th className="px-4 py-3 text-left text-sm font-medium text-muted-foreground">
                        Date
                      </th>
                      <th className="px-4 py-3 text-left text-sm font-medium text-muted-foreground">
                        Bank Account
                      </th>
                    </tr>
                  </thead>
                  <tbody>
                    {settlements.map((settlement) => (
                      <tr key={settlement.id} className="border-b">
                        <td className="px-4 py-3">
                          <span className="font-medium">{settlement.settlementNumber}</span>
                        </td>
                        <td className="px-4 py-3">
                          <div>
                            <div className="font-medium">
                              {formatCurrency(settlement.netAmount)}
                            </div>
                            <div className="text-xs text-muted-foreground">
                              Gross: {formatCurrency(settlement.grossAmount)}
                            </div>
                          </div>
                        </td>
                        <td className="px-4 py-3">
                          <span
                            className={`inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-medium ${
                              statusColors[settlement.status]
                            }`}
                          >
                            {settlement.status}
                          </span>
                        </td>
                        <td className="px-4 py-3 text-sm">
                          <div>{formatDate(settlement.createdAt)}</div>
                          {settlement.paidAt && (
                            <div className="text-xs text-muted-foreground">
                              Paid: {formatDate(settlement.paidAt)}
                            </div>
                          )}
                        </td>
                        <td className="px-4 py-3">
                          <div className="flex items-center gap-2">
                            <Building2 className="h-4 w-4 text-muted-foreground" />
                            <div>
                              <div className="text-sm">{settlement.bankName}</div>
                              <div className="text-xs text-muted-foreground">
                                ****{settlement.bankAccountLast4}
                              </div>
                            </div>
                          </div>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>

              {settlements.length === 0 && !isLoading && (
                <div className="p-8 text-center text-muted-foreground">
                  No settlements found matching your filters.
                </div>
              )}
            </CardContent>
          </Card>
        )}
      </div>
    </div>
  );
}
